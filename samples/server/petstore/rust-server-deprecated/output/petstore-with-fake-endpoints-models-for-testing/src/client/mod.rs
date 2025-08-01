#![allow(clippy::clone_on_copy)]
#![allow(clippy::vec_init_then_push)]
use async_trait::async_trait;
use futures::{Stream, future, future::BoxFuture, stream, future::TryFutureExt, future::FutureExt, stream::StreamExt};
use hyper::header::{HeaderName, HeaderValue, CONTENT_TYPE};
use hyper::{Body, Request, Response, service::Service, Uri};
use percent_encoding::{utf8_percent_encode, AsciiSet};
use std::borrow::Cow;
use std::convert::TryInto;
use std::io::{ErrorKind, Read};
use std::error::Error;
use std::future::Future;
use std::fmt;
use std::marker::PhantomData;
use std::path::Path;
use std::sync::{Arc, Mutex};
use std::str;
use std::str::FromStr;
use std::string::ToString;
use std::task::{Context, Poll};
use swagger::{ApiError, AuthData, BodyExt, Connector, DropContextService, Has, XSpanIdString};
use url::form_urlencoded;

use mime::Mime;
use std::io::Cursor;
use multipart::client::lazy::Multipart;

use crate::models;
use crate::header;

/// https://url.spec.whatwg.org/#fragment-percent-encode-set
#[allow(dead_code)]
const FRAGMENT_ENCODE_SET: &AsciiSet = &percent_encoding::CONTROLS
    .add(b' ').add(b'"').add(b'<').add(b'>').add(b'`');

/// This encode set is used for object IDs
///
/// Aside from the special characters defined in the `PATH_SEGMENT_ENCODE_SET`,
/// the vertical bar (|) is encoded.
#[allow(dead_code)]
const ID_ENCODE_SET: &AsciiSet = &FRAGMENT_ENCODE_SET.add(b'|');

use crate::{Api,
     TestSpecialTagsResponse,
     Call123exampleResponse,
     FakeOuterBooleanSerializeResponse,
     FakeOuterCompositeSerializeResponse,
     FakeOuterNumberSerializeResponse,
     FakeOuterStringSerializeResponse,
     FakeResponseWithNumericalDescriptionResponse,
     TestBodyWithQueryParamsResponse,
     TestClientModelResponse,
     TestEndpointParametersResponse,
     TestEnumParametersResponse,
     TestInlineAdditionalPropertiesResponse,
     TestJsonFormDataResponse,
     HyphenParamResponse,
     TestClassnameResponse,
     AddPetResponse,
     FindPetsByStatusResponse,
     FindPetsByTagsResponse,
     UpdatePetResponse,
     DeletePetResponse,
     GetPetByIdResponse,
     UpdatePetWithFormResponse,
     UploadFileResponse,
     GetInventoryResponse,
     PlaceOrderResponse,
     DeleteOrderResponse,
     GetOrderByIdResponse,
     CreateUserResponse,
     CreateUsersWithArrayInputResponse,
     CreateUsersWithListInputResponse,
     LoginUserResponse,
     LogoutUserResponse,
     DeleteUserResponse,
     GetUserByNameResponse,
     UpdateUserResponse
     };

/// Convert input into a base path, e.g. "http://example:123". Also checks the scheme as it goes.
fn into_base_path(input: impl TryInto<Uri, Error=hyper::http::uri::InvalidUri>, correct_scheme: Option<&'static str>) -> Result<String, ClientInitError> {
    // First convert to Uri, since a base path is a subset of Uri.
    let uri = input.try_into()?;

    let scheme = uri.scheme_str().ok_or(ClientInitError::InvalidScheme)?;

    // Check the scheme if necessary
    if let Some(correct_scheme) = correct_scheme {
        if scheme != correct_scheme {
            return Err(ClientInitError::InvalidScheme);
        }
    }

    let host = uri.host().ok_or(ClientInitError::MissingHost)?;
    let port = uri.port_u16().map(|x| format!(":{x}")).unwrap_or_default();
    Ok(format!("{scheme}://{host}{port}{}", uri.path().trim_end_matches('/')))
}

/// A client that implements the API by making HTTP calls out to a server.
pub struct Client<S, C> where
    S: Service<
           (Request<Body>, C),
           Response=Response<Body>> + Clone + Sync + Send + 'static,
    S::Future: Send + 'static,
    S::Error: Into<crate::ServiceError> + fmt::Display,
    C: Clone + Send + Sync + 'static
{
    /// Inner service
    client_service: S,

    /// Base path of the API
    base_path: String,

    /// Marker
    marker: PhantomData<fn(C)>,
}

impl<S, C> fmt::Debug for Client<S, C> where
    S: Service<
           (Request<Body>, C),
           Response=Response<Body>> + Clone + Sync + Send + 'static,
    S::Future: Send + 'static,
    S::Error: Into<crate::ServiceError> + fmt::Display,
    C: Clone + Send + Sync + 'static
{
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        write!(f, "Client {{ base_path: {} }}", self.base_path)
    }
}

impl<S, C> Clone for Client<S, C> where
    S: Service<
           (Request<Body>, C),
           Response=Response<Body>> + Clone + Sync + Send + 'static,
    S::Future: Send + 'static,
    S::Error: Into<crate::ServiceError> + fmt::Display,
    C: Clone + Send + Sync + 'static
{
    fn clone(&self) -> Self {
        Self {
            client_service: self.client_service.clone(),
            base_path: self.base_path.clone(),
            marker: PhantomData,
        }
    }
}

impl<Connector, C> Client<DropContextService<hyper::client::Client<Connector, Body>, C>, C> where
    Connector: hyper::client::connect::Connect + Clone + Send + Sync + 'static,
    C: Clone + Send + Sync + 'static,
{
    /// Create a client with a custom implementation of hyper::client::Connect.
    ///
    /// Intended for use with custom implementations of connect for e.g. protocol logging
    /// or similar functionality which requires wrapping the transport layer. When wrapping a TCP connection,
    /// this function should be used in conjunction with `swagger::Connector::builder()`.
    ///
    /// For ordinary tcp connections, prefer the use of `try_new_http`, `try_new_https`
    /// and `try_new_https_mutual`, to avoid introducing a dependency on the underlying transport layer.
    ///
    /// # Arguments
    ///
    /// * `base_path` - base path of the client API, i.e. "http://www.my-api-implementation.com"
    /// * `protocol` - Which protocol to use when constructing the request url, e.g. `Some("http")`
    /// * `connector` - Implementation of `hyper::client::Connect` to use for the client
    pub fn try_new_with_connector(
        base_path: &str,
        protocol: Option<&'static str>,
        connector: Connector,
    ) -> Result<Self, ClientInitError>
    {
        let client_service = hyper::client::Client::builder().build(connector);
        let client_service = DropContextService::new(client_service);

        Ok(Self {
            client_service,
            base_path: into_base_path(base_path, protocol)?,
            marker: PhantomData,
        })
    }
}

#[derive(Debug, Clone)]
pub enum HyperClient {
    Http(hyper::client::Client<hyper::client::HttpConnector, Body>),
    Https(hyper::client::Client<HttpsConnector, Body>),
}

impl Service<Request<Body>> for HyperClient {
    type Response = Response<Body>;
    type Error = hyper::Error;
    type Future = hyper::client::ResponseFuture;

    fn poll_ready(&mut self, cx: &mut Context) -> Poll<Result<(), Self::Error>> {
       match self {
          HyperClient::Http(client) => client.poll_ready(cx),
          HyperClient::Https(client) => client.poll_ready(cx),
       }
    }

    fn call(&mut self, req: Request<Body>) -> Self::Future {
       match self {
          HyperClient::Http(client) => client.call(req),
          HyperClient::Https(client) => client.call(req)
       }
    }
}

impl<C> Client<DropContextService<HyperClient, C>, C> where
    C: Clone + Send + Sync + 'static,
{
    /// Create an HTTP client.
    ///
    /// # Arguments
    /// * `base_path` - base path of the client API, i.e. "http://www.my-api-implementation.com"
    pub fn try_new(
        base_path: &str,
    ) -> Result<Self, ClientInitError> {
        let uri = Uri::from_str(base_path)?;

        let scheme = uri.scheme_str().ok_or(ClientInitError::InvalidScheme)?;
        let scheme = scheme.to_ascii_lowercase();

        let connector = Connector::builder();

        let client_service = match scheme.as_str() {
            "http" => {
                HyperClient::Http(hyper::client::Client::builder().build(connector.build()))
            },
            "https" => {
                let connector = connector.https()
                   .build()
                   .map_err(ClientInitError::SslError)?;
                HyperClient::Https(hyper::client::Client::builder().build(connector))
            },
            _ => {
                return Err(ClientInitError::InvalidScheme);
            }
        };

        let client_service = DropContextService::new(client_service);

        Ok(Self {
            client_service,
            base_path: into_base_path(base_path, None)?,
            marker: PhantomData,
        })
    }
}

impl<C> Client<DropContextService<hyper::client::Client<hyper::client::HttpConnector, Body>, C>, C> where
    C: Clone + Send + Sync + 'static
{
    /// Create an HTTP client.
    ///
    /// # Arguments
    /// * `base_path` - base path of the client API, i.e. "http://www.my-api-implementation.com"
    pub fn try_new_http(
        base_path: &str,
    ) -> Result<Self, ClientInitError> {
        let http_connector = Connector::builder().build();

        Self::try_new_with_connector(base_path, Some("http"), http_connector)
    }
}

#[cfg(any(target_os = "macos", target_os = "windows", target_os = "ios"))]
type HttpsConnector = hyper_tls::HttpsConnector<hyper::client::HttpConnector>;

#[cfg(not(any(target_os = "macos", target_os = "windows", target_os = "ios")))]
type HttpsConnector = hyper_openssl::HttpsConnector<hyper::client::HttpConnector>;

impl<C> Client<DropContextService<hyper::client::Client<HttpsConnector, Body>, C>, C> where
    C: Clone + Send + Sync + 'static
{
    /// Create a client with a TLS connection to the server
    ///
    /// # Arguments
    /// * `base_path` - base path of the client API, i.e. "<https://www.my-api-implementation.com>"
    pub fn try_new_https(base_path: &str) -> Result<Self, ClientInitError>
    {
        let https_connector = Connector::builder()
            .https()
            .build()
            .map_err(ClientInitError::SslError)?;
        Self::try_new_with_connector(base_path, Some("https"), https_connector)
    }

    /// Create a client with a TLS connection to the server using a pinned certificate
    ///
    /// # Arguments
    /// * `base_path` - base path of the client API, i.e. "<https://www.my-api-implementation.com>"
    /// * `ca_certificate` - Path to CA certificate used to authenticate the server
    #[cfg(not(any(target_os = "macos", target_os = "windows", target_os = "ios")))]
    pub fn try_new_https_pinned<CA>(
        base_path: &str,
        ca_certificate: CA,
    ) -> Result<Self, ClientInitError>
    where
        CA: AsRef<Path>,
    {
        let https_connector = Connector::builder()
            .https()
            .pin_server_certificate(ca_certificate)
            .build()
            .map_err(ClientInitError::SslError)?;
        Self::try_new_with_connector(base_path, Some("https"), https_connector)
    }

    /// Create a client with a mutually authenticated TLS connection to the server.
    ///
    /// # Arguments
    /// * `base_path` - base path of the client API, i.e. "<https://www.my-api-implementation.com>"
    /// * `ca_certificate` - Path to CA certificate used to authenticate the server
    /// * `client_key` - Path to the client private key
    /// * `client_certificate` - Path to the client's public certificate associated with the private key
    #[cfg(not(any(target_os = "macos", target_os = "windows", target_os = "ios")))]
    pub fn try_new_https_mutual<CA, K, D>(
        base_path: &str,
        ca_certificate: CA,
        client_key: K,
        client_certificate: D,
    ) -> Result<Self, ClientInitError>
    where
        CA: AsRef<Path>,
        K: AsRef<Path>,
        D: AsRef<Path>,
    {
        let https_connector = Connector::builder()
            .https()
            .pin_server_certificate(ca_certificate)
            .client_authentication(client_key, client_certificate)
            .build()
            .map_err(ClientInitError::SslError)?;
        Self::try_new_with_connector(base_path, Some("https"), https_connector)
    }
}

impl<S, C> Client<S, C> where
    S: Service<
           (Request<Body>, C),
           Response=Response<Body>> + Clone + Sync + Send + 'static,
    S::Future: Send + 'static,
    S::Error: Into<crate::ServiceError> + fmt::Display,
    C: Clone + Send + Sync + 'static
{
    /// Constructor for creating a `Client` by passing in a pre-made `hyper::service::Service` /
    /// `tower::Service`
    ///
    /// This allows adding custom wrappers around the underlying transport, for example for logging.
    pub fn try_new_with_client_service(
        client_service: S,
        base_path: &str,
    ) -> Result<Self, ClientInitError>
    {
        Ok(Self {
            client_service,
            base_path: into_base_path(base_path, None)?,
            marker: PhantomData,
        })
    }
}

/// Error type failing to create a Client
#[derive(Debug)]
pub enum ClientInitError {
    /// Invalid URL Scheme
    InvalidScheme,

    /// Invalid URI
    InvalidUri(hyper::http::uri::InvalidUri),

    /// Missing Hostname
    MissingHost,

    /// SSL Connection Error
    #[cfg(any(target_os = "macos", target_os = "windows", target_os = "ios"))]
    SslError(native_tls::Error),

    /// SSL Connection Error
    #[cfg(not(any(target_os = "macos", target_os = "windows", target_os = "ios")))]
    SslError(openssl::error::ErrorStack),
}

impl From<hyper::http::uri::InvalidUri> for ClientInitError {
    fn from(err: hyper::http::uri::InvalidUri) -> ClientInitError {
        ClientInitError::InvalidUri(err)
    }
}

impl fmt::Display for ClientInitError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let s: &dyn fmt::Debug = self;
        s.fmt(f)
    }
}

impl Error for ClientInitError {
    fn description(&self) -> &str {
        "Failed to produce a hyper client."
    }
}

#[async_trait]
impl<S, C> Api<C> for Client<S, C> where
    S: Service<
       (Request<Body>, C),
       Response=Response<Body>> + Clone + Sync + Send + 'static,
    S::Future: Send + 'static,
    S::Error: Into<crate::ServiceError> + fmt::Display,
    C: Has<XSpanIdString> + Has<Option<AuthData>> + Clone + Send + Sync + 'static,
{
    fn poll_ready(&self, cx: &mut Context) -> Poll<Result<(), crate::ServiceError>> {
        match self.client_service.clone().poll_ready(cx) {
            Poll::Ready(Err(e)) => Poll::Ready(Err(e.into())),
            Poll::Ready(Ok(o)) => Poll::Ready(Ok(o)),
            Poll::Pending => Poll::Pending,
        }
    }

    async fn test_special_tags(
        &self,
        param_body: models::Client,
        context: &C) -> Result<TestSpecialTagsResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/another-fake/dummy",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("PATCH")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                let body = serde_json::from_str::<models::Client>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(TestSpecialTagsResponse::SuccessfulOperation
                    (body)
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn call123example(
        &self,
        context: &C) -> Result<Call123exampleResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake/operation-with-numeric-id",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                Ok(
                    Call123exampleResponse::Success
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn fake_outer_boolean_serialize(
        &self,
        param_body: Option<models::OuterBoolean>,
        context: &C) -> Result<FakeOuterBooleanSerializeResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake/outer/boolean",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        if let Some(param_body) = param_body {
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);
        }

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                let body = serde_json::from_str::<bool>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(FakeOuterBooleanSerializeResponse::OutputBoolean
                    (body)
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn fake_outer_composite_serialize(
        &self,
        param_body: Option<models::OuterComposite>,
        context: &C) -> Result<FakeOuterCompositeSerializeResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake/outer/composite",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        if let Some(param_body) = param_body {
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);
        }

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                let body = serde_json::from_str::<models::OuterComposite>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(FakeOuterCompositeSerializeResponse::OutputComposite
                    (body)
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn fake_outer_number_serialize(
        &self,
        param_body: Option<models::OuterNumber>,
        context: &C) -> Result<FakeOuterNumberSerializeResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake/outer/number",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        if let Some(param_body) = param_body {
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);
        }

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                let body = serde_json::from_str::<f64>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(FakeOuterNumberSerializeResponse::OutputNumber
                    (body)
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn fake_outer_string_serialize(
        &self,
        param_body: Option<models::OuterString>,
        context: &C) -> Result<FakeOuterStringSerializeResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake/outer/string",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        if let Some(param_body) = param_body {
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);
        }

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                let body = serde_json::from_str::<String>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(FakeOuterStringSerializeResponse::OutputString
                    (body)
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn fake_response_with_numerical_description(
        &self,
        context: &C) -> Result<FakeResponseWithNumericalDescriptionResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake/response-with-numerical-description",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                Ok(
                    FakeResponseWithNumericalDescriptionResponse::Status200
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn test_body_with_query_params(
        &self,
        param_query: String,
        param_body: models::User,
        context: &C) -> Result<TestBodyWithQueryParamsResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake/body-with-query-params",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
                query_string.append_pair("query",
                    &param_query);
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("PUT")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                Ok(
                    TestBodyWithQueryParamsResponse::Success
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn test_client_model(
        &self,
        param_body: models::Client,
        context: &C) -> Result<TestClientModelResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("PATCH")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                let body = serde_json::from_str::<models::Client>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(TestClientModelResponse::SuccessfulOperation
                    (body)
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn test_endpoint_parameters(
        &self,
        param_number: f64,
        param_double: f64,
        param_pattern_without_delimiter: String,
        param_byte: swagger::ByteArray,
        param_integer: Option<i32>,
        param_int32: Option<i32>,
        param_int64: Option<i64>,
        param_float: Option<f32>,
        param_string: Option<String>,
        param_binary: Option<swagger::ByteArray>,
        param_date: Option<chrono::naive::NaiveDate>,
        param_date_time: Option<chrono::DateTime::<chrono::Utc>>,
        param_password: Option<String>,
        param_callback: Option<String>,
        context: &C) -> Result<TestEndpointParametersResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes form body
        let mut params = vec![];
        if let Some(param_integer) = param_integer {
        #[allow(clippy::uninlined_format_args)]
        params.push(("integer",
            format!("{:?}", param_integer)
        ));
        }
        if let Some(param_int32) = param_int32 {
        #[allow(clippy::uninlined_format_args)]
        params.push(("int32",
            format!("{:?}", param_int32)
        ));
        }
        if let Some(param_int64) = param_int64 {
        #[allow(clippy::uninlined_format_args)]
        params.push(("int64",
            format!("{:?}", param_int64)
        ));
        }
        #[allow(clippy::uninlined_format_args)]
        params.push(("number",
            format!("{}", param_number)
        ));
        if let Some(param_float) = param_float {
        #[allow(clippy::uninlined_format_args)]
        params.push(("float",
            format!("{:?}", param_float)
        ));
        }
        #[allow(clippy::uninlined_format_args)]
        params.push(("double",
            format!("{}", param_double)
        ));
        if let Some(param_string) = param_string {
        #[allow(clippy::uninlined_format_args)]
        params.push(("string",
            param_string
        ));
        }
        #[allow(clippy::uninlined_format_args)]
        params.push(("pattern_without_delimiter",
            param_pattern_without_delimiter
        ));
        #[allow(clippy::uninlined_format_args)]
        params.push(("byte",
            format!("{:?}", param_byte)
        ));
        if let Some(param_binary) = param_binary {
        #[allow(clippy::uninlined_format_args)]
        params.push(("binary",
            format!("{:?}", param_binary)
        ));
        }
        if let Some(param_date) = param_date {
        #[allow(clippy::uninlined_format_args)]
        params.push(("date",
            format!("{:?}", param_date)
        ));
        }
        if let Some(param_date_time) = param_date_time {
        #[allow(clippy::uninlined_format_args)]
        params.push(("dateTime",
            format!("{:?}", param_date_time)
        ));
        }
        if let Some(param_password) = param_password {
        #[allow(clippy::uninlined_format_args)]
        params.push(("password",
            param_password
        ));
        }
        if let Some(param_callback) = param_callback {
        #[allow(clippy::uninlined_format_args)]
        params.push(("callback",
            param_callback
        ));
        }

        let body = serde_urlencoded::to_string(params).expect("impossible to fail to serialize");

        *request.body_mut() = Body::from(body.into_bytes());

        let header = "application/x-www-form-urlencoded";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        #[allow(clippy::collapsible_match)]
        if let Some(auth_data) = Has::<Option<AuthData>>::get(context).as_ref() {
            #[allow(clippy::single_match, clippy::match_single_binding)]
            match auth_data {
                AuthData::Basic(basic_header) => {
                    let auth = swagger::auth::Header(basic_header.clone());
                    let header = match HeaderValue::from_str(&format!("{auth}")) {
                        Ok(h) => h,
                        Err(e) => return Err(ApiError(format!("Unable to create Authorization header: {e}")))
                    };
                    request.headers_mut().insert(
                        hyper::header::AUTHORIZATION,
                        header);
                },
                _ => {}
            }
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            400 => {
                Ok(
                    TestEndpointParametersResponse::InvalidUsernameSupplied
                )
            }
            404 => {
                Ok(
                    TestEndpointParametersResponse::UserNotFound
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn test_enum_parameters(
        &self,
        param_enum_header_string_array: Option<&Vec<models::TestEnumParametersEnumHeaderStringArrayParameterInner>>,
        param_enum_header_string: Option<models::TestEnumParametersEnumHeaderStringParameter>,
        param_enum_query_string_array: Option<&Vec<models::TestEnumParametersEnumHeaderStringArrayParameterInner>>,
        param_enum_query_string: Option<models::TestEnumParametersEnumHeaderStringParameter>,
        param_enum_query_integer: Option<models::TestEnumParametersEnumQueryIntegerParameter>,
        param_enum_query_double: Option<models::TestEnumParametersEnumQueryDoubleParameter>,
        param_enum_form_string: Option<models::TestEnumParametersRequestEnumFormString>,
        context: &C) -> Result<TestEnumParametersResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            if let Some(param_enum_query_string_array) = param_enum_query_string_array {
                query_string.append_pair("enum_query_string_array",
                    &param_enum_query_string_array.iter().map(ToString::to_string).collect::<Vec<String>>().join(","));
            }
            if let Some(param_enum_query_string) = param_enum_query_string {
                query_string.append_pair("enum_query_string",
                    &param_enum_query_string.to_string());
            }
            if let Some(param_enum_query_integer) = param_enum_query_integer {
                query_string.append_pair("enum_query_integer",
                    &param_enum_query_integer.to_string());
            }
            if let Some(param_enum_query_double) = param_enum_query_double {
                query_string.append_pair("enum_query_double",
                    &param_enum_query_double.to_string());
            }
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes form body
        let mut params = vec![];
        if let Some(param_enum_form_string) = param_enum_form_string {
        #[allow(clippy::uninlined_format_args)]
        params.push(("enum_form_string",
            format!("{:?}", param_enum_form_string)
        ));
        }

        let body = serde_urlencoded::to_string(params).expect("impossible to fail to serialize");

        *request.body_mut() = Body::from(body.into_bytes());

        let header = "application/x-www-form-urlencoded";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        // Header parameters
        #[allow(clippy::single_match)]
        match param_enum_header_string_array {
            Some(param_enum_header_string_array) => {
        request.headers_mut().append(
            HeaderName::from_static("enum_header_string_array"),
            #[allow(clippy::redundant_clone)]
            match header::IntoHeaderValue(param_enum_header_string_array.clone()).try_into() {
                Ok(header) => header,
                Err(e) => {
                    return Err(ApiError(format!(
                        "Invalid header enum_header_string_array - {e}")));
                },
            });
            },
            None => {}
        }

        #[allow(clippy::single_match)]
        match param_enum_header_string {
            Some(param_enum_header_string) => {
        request.headers_mut().append(
            HeaderName::from_static("enum_header_string"),
            #[allow(clippy::redundant_clone)]
            match header::IntoHeaderValue(param_enum_header_string.clone()).try_into() {
                Ok(header) => header,
                Err(e) => {
                    return Err(ApiError(format!(
                        "Invalid header enum_header_string - {e}")));
                },
            });
            },
            None => {}
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            400 => {
                Ok(
                    TestEnumParametersResponse::InvalidRequest
                )
            }
            404 => {
                Ok(
                    TestEnumParametersResponse::NotFound
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn test_inline_additional_properties(
        &self,
        param_param: std::collections::HashMap<String, String>,
        context: &C) -> Result<TestInlineAdditionalPropertiesResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake/inline-additionalProperties",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = serde_json::to_string(&param_param).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                Ok(
                    TestInlineAdditionalPropertiesResponse::SuccessfulOperation
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn test_json_form_data(
        &self,
        param_param: String,
        param_param2: String,
        context: &C) -> Result<TestJsonFormDataResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake/jsonFormData",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes form body
        let mut params = vec![];
        #[allow(clippy::uninlined_format_args)]
        params.push(("param",
            param_param
        ));
        #[allow(clippy::uninlined_format_args)]
        params.push(("param2",
            param_param2
        ));

        let body = serde_urlencoded::to_string(params).expect("impossible to fail to serialize");

        *request.body_mut() = Body::from(body.into_bytes());

        let header = "application/x-www-form-urlencoded";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                Ok(
                    TestJsonFormDataResponse::SuccessfulOperation
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn hyphen_param(
        &self,
        param_hyphen_param: String,
        context: &C) -> Result<HyphenParamResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake/hyphenParam/{hyphen_param}",
            self.base_path
            ,hyphen_param=utf8_percent_encode(&param_hyphen_param.to_string(), ID_ENCODE_SET)
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                Ok(
                    HyphenParamResponse::Success
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn test_classname(
        &self,
        param_body: models::Client,
        context: &C) -> Result<TestClassnameResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/fake_classname_test",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            if let Some(AuthData::ApiKey(ref api_key)) = (context as &dyn Has<Option<AuthData>>).get().as_ref() {
                query_string.append_pair("api_key_query", api_key);
            }
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("PATCH")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        #[allow(clippy::collapsible_match)]
        if let Some(auth_data) = Has::<Option<AuthData>>::get(context).as_ref() {
            #[allow(clippy::single_match, clippy::match_single_binding)]
            match auth_data {
                _ => {}
            }
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                let body = serde_json::from_str::<models::Client>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(TestClassnameResponse::SuccessfulOperation
                    (body)
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn add_pet(
        &self,
        param_body: models::Pet,
        context: &C) -> Result<AddPetResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/pet",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = param_body.as_xml();
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        #[allow(clippy::collapsible_match)]
        if let Some(auth_data) = Has::<Option<AuthData>>::get(context).as_ref() {
            #[allow(clippy::single_match, clippy::match_single_binding)]
            match auth_data {
                AuthData::Bearer(bearer_header) => {
                    let auth = swagger::auth::Header(bearer_header.clone());
                    let header = match HeaderValue::from_str(&format!("{auth}")) {
                        Ok(h) => h,
                        Err(e) => return Err(ApiError(format!("Unable to create Authorization header: {e}")))
                    };
                    request.headers_mut().insert(
                        hyper::header::AUTHORIZATION,
                        header);
                },
                _ => {}
            }
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            405 => {
                Ok(
                    AddPetResponse::InvalidInput
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn find_pets_by_status(
        &self,
        param_status: &Vec<models::FindPetsByStatusStatusParameterInner>,
        context: &C) -> Result<FindPetsByStatusResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/pet/findByStatus",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
                query_string.append_pair("status",
                    &param_status.iter().map(ToString::to_string).collect::<Vec<String>>().join(","));
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        #[allow(clippy::collapsible_match)]
        if let Some(auth_data) = Has::<Option<AuthData>>::get(context).as_ref() {
            #[allow(clippy::single_match, clippy::match_single_binding)]
            match auth_data {
                AuthData::Bearer(bearer_header) => {
                    let auth = swagger::auth::Header(bearer_header.clone());
                    let header = match HeaderValue::from_str(&format!("{auth}")) {
                        Ok(h) => h,
                        Err(e) => return Err(ApiError(format!("Unable to create Authorization header: {e}")))
                    };
                    request.headers_mut().insert(
                        hyper::header::AUTHORIZATION,
                        header);
                },
                _ => {}
            }
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                // ToDo: this will move to swagger-rs and become a standard From conversion trait
                // once https://github.com/RReverser/serde-xml-rs/pull/45 is accepted upstream
                let body = serde_xml_rs::from_str::<Vec<models::Pet>>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(FindPetsByStatusResponse::SuccessfulOperation
                    (body)
                )
            }
            400 => {
                Ok(
                    FindPetsByStatusResponse::InvalidStatusValue
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn find_pets_by_tags(
        &self,
        param_tags: &Vec<String>,
        context: &C) -> Result<FindPetsByTagsResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/pet/findByTags",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
                query_string.append_pair("tags",
                    &param_tags.iter().map(ToString::to_string).collect::<Vec<String>>().join(","));
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        #[allow(clippy::collapsible_match)]
        if let Some(auth_data) = Has::<Option<AuthData>>::get(context).as_ref() {
            #[allow(clippy::single_match, clippy::match_single_binding)]
            match auth_data {
                AuthData::Bearer(bearer_header) => {
                    let auth = swagger::auth::Header(bearer_header.clone());
                    let header = match HeaderValue::from_str(&format!("{auth}")) {
                        Ok(h) => h,
                        Err(e) => return Err(ApiError(format!("Unable to create Authorization header: {e}")))
                    };
                    request.headers_mut().insert(
                        hyper::header::AUTHORIZATION,
                        header);
                },
                _ => {}
            }
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                // ToDo: this will move to swagger-rs and become a standard From conversion trait
                // once https://github.com/RReverser/serde-xml-rs/pull/45 is accepted upstream
                let body = serde_xml_rs::from_str::<Vec<models::Pet>>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(FindPetsByTagsResponse::SuccessfulOperation
                    (body)
                )
            }
            400 => {
                Ok(
                    FindPetsByTagsResponse::InvalidTagValue
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn update_pet(
        &self,
        param_body: models::Pet,
        context: &C) -> Result<UpdatePetResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/pet",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("PUT")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = param_body.as_xml();
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        #[allow(clippy::collapsible_match)]
        if let Some(auth_data) = Has::<Option<AuthData>>::get(context).as_ref() {
            #[allow(clippy::single_match, clippy::match_single_binding)]
            match auth_data {
                AuthData::Bearer(bearer_header) => {
                    let auth = swagger::auth::Header(bearer_header.clone());
                    let header = match HeaderValue::from_str(&format!("{auth}")) {
                        Ok(h) => h,
                        Err(e) => return Err(ApiError(format!("Unable to create Authorization header: {e}")))
                    };
                    request.headers_mut().insert(
                        hyper::header::AUTHORIZATION,
                        header);
                },
                _ => {}
            }
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            400 => {
                Ok(
                    UpdatePetResponse::InvalidIDSupplied
                )
            }
            404 => {
                Ok(
                    UpdatePetResponse::PetNotFound
                )
            }
            405 => {
                Ok(
                    UpdatePetResponse::ValidationException
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn delete_pet(
        &self,
        param_pet_id: i64,
        param_api_key: Option<String>,
        context: &C) -> Result<DeletePetResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/pet/{pet_id}",
            self.base_path
            ,pet_id=utf8_percent_encode(&param_pet_id.to_string(), ID_ENCODE_SET)
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("DELETE")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        #[allow(clippy::collapsible_match)]
        if let Some(auth_data) = Has::<Option<AuthData>>::get(context).as_ref() {
            #[allow(clippy::single_match, clippy::match_single_binding)]
            match auth_data {
                AuthData::Bearer(bearer_header) => {
                    let auth = swagger::auth::Header(bearer_header.clone());
                    let header = match HeaderValue::from_str(&format!("{auth}")) {
                        Ok(h) => h,
                        Err(e) => return Err(ApiError(format!("Unable to create Authorization header: {e}")))
                    };
                    request.headers_mut().insert(
                        hyper::header::AUTHORIZATION,
                        header);
                },
                _ => {}
            }
        }

        // Header parameters
        #[allow(clippy::single_match)]
        match param_api_key {
            Some(param_api_key) => {
        request.headers_mut().append(
            HeaderName::from_static("api_key"),
            #[allow(clippy::redundant_clone)]
            match header::IntoHeaderValue(param_api_key.clone()).try_into() {
                Ok(header) => header,
                Err(e) => {
                    return Err(ApiError(format!(
                        "Invalid header api_key - {e}")));
                },
            });
            },
            None => {}
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            400 => {
                Ok(
                    DeletePetResponse::InvalidPetValue
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn get_pet_by_id(
        &self,
        param_pet_id: i64,
        context: &C) -> Result<GetPetByIdResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/pet/{pet_id}",
            self.base_path
            ,pet_id=utf8_percent_encode(&param_pet_id.to_string(), ID_ENCODE_SET)
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        #[allow(clippy::collapsible_match)]
        if let Some(auth_data) = Has::<Option<AuthData>>::get(context).as_ref() {
            #[allow(clippy::single_match, clippy::match_single_binding)]
            match auth_data {
                _ => {}
            }
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                // ToDo: this will move to swagger-rs and become a standard From conversion trait
                // once https://github.com/RReverser/serde-xml-rs/pull/45 is accepted upstream
                let body = serde_xml_rs::from_str::<models::Pet>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(GetPetByIdResponse::SuccessfulOperation
                    (body)
                )
            }
            400 => {
                Ok(
                    GetPetByIdResponse::InvalidIDSupplied
                )
            }
            404 => {
                Ok(
                    GetPetByIdResponse::PetNotFound
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn update_pet_with_form(
        &self,
        param_pet_id: i64,
        param_name: Option<String>,
        param_status: Option<String>,
        context: &C) -> Result<UpdatePetWithFormResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/pet/{pet_id}",
            self.base_path
            ,pet_id=utf8_percent_encode(&param_pet_id.to_string(), ID_ENCODE_SET)
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes form body
        let mut params = vec![];
        if let Some(param_name) = param_name {
        #[allow(clippy::uninlined_format_args)]
        params.push(("name",
            param_name
        ));
        }
        if let Some(param_status) = param_status {
        #[allow(clippy::uninlined_format_args)]
        params.push(("status",
            param_status
        ));
        }

        let body = serde_urlencoded::to_string(params).expect("impossible to fail to serialize");

        *request.body_mut() = Body::from(body.into_bytes());

        let header = "application/x-www-form-urlencoded";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        #[allow(clippy::collapsible_match)]
        if let Some(auth_data) = Has::<Option<AuthData>>::get(context).as_ref() {
            #[allow(clippy::single_match, clippy::match_single_binding)]
            match auth_data {
                AuthData::Bearer(bearer_header) => {
                    let auth = swagger::auth::Header(bearer_header.clone());
                    let header = match HeaderValue::from_str(&format!("{auth}")) {
                        Ok(h) => h,
                        Err(e) => return Err(ApiError(format!("Unable to create Authorization header: {e}")))
                    };
                    request.headers_mut().insert(
                        hyper::header::AUTHORIZATION,
                        header);
                },
                _ => {}
            }
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            405 => {
                Ok(
                    UpdatePetWithFormResponse::InvalidInput
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn upload_file(
        &self,
        param_pet_id: i64,
        param_additional_metadata: Option<String>,
        param_file: Option<swagger::ByteArray>,
        context: &C) -> Result<UploadFileResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/pet/{pet_id}/uploadImage",
            self.base_path
            ,pet_id=utf8_percent_encode(&param_pet_id.to_string(), ID_ENCODE_SET)
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes multipart/form body
        let (body_string, multipart_header) = {
            let mut multipart = Multipart::new();

            // For each parameter, encode as appropriate and add to the multipart body as a stream.

            let additional_metadata_str = match serde_json::to_string(&param_additional_metadata) {
                Ok(str) => str,
                Err(e) => return Err(ApiError(format!("Unable to serialize additional_metadata to string: {e}"))),
            };

            let additional_metadata_vec = additional_metadata_str.as_bytes().to_vec();
            let additional_metadata_mime = mime_0_2::Mime::from_str("application/json").expect("impossible to fail to parse");
            let additional_metadata_cursor = Cursor::new(additional_metadata_vec);

            multipart.add_stream("additional_metadata",  additional_metadata_cursor,  None as Option<&str>, Some(additional_metadata_mime));


            let file_str = match serde_json::to_string(&param_file) {
                Ok(str) => str,
                Err(e) => return Err(ApiError(format!("Unable to serialize file to string: {e}"))),
            };

            let file_vec = file_str.as_bytes().to_vec();
            let file_mime = mime_0_2::Mime::from_str("application/json").expect("impossible to fail to parse");
            let file_cursor = Cursor::new(file_vec);

            multipart.add_stream("file",  file_cursor,  None as Option<&str>, Some(file_mime));


            let mut fields = match multipart.prepare() {
                Ok(fields) => fields,
                Err(err) => return Err(ApiError(format!("Unable to build request: {err}"))),
            };

            let mut body_string = String::new();

            match fields.read_to_string(&mut body_string) {
                Ok(_) => (),
                Err(err) => return Err(ApiError(format!("Unable to build body: {err}"))),
            }

            let boundary = fields.boundary();

            let multipart_header = format!("multipart/form-data;{boundary}");

            (body_string, multipart_header)
        };

        *request.body_mut() = Body::from(body_string);

        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(&multipart_header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {multipart_header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        #[allow(clippy::collapsible_match)]
        if let Some(auth_data) = Has::<Option<AuthData>>::get(context).as_ref() {
            #[allow(clippy::single_match, clippy::match_single_binding)]
            match auth_data {
                AuthData::Bearer(bearer_header) => {
                    let auth = swagger::auth::Header(bearer_header.clone());
                    let header = match HeaderValue::from_str(&format!("{auth}")) {
                        Ok(h) => h,
                        Err(e) => return Err(ApiError(format!("Unable to create Authorization header: {e}")))
                    };
                    request.headers_mut().insert(
                        hyper::header::AUTHORIZATION,
                        header);
                },
                _ => {}
            }
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                let body = serde_json::from_str::<models::ApiResponse>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(UploadFileResponse::SuccessfulOperation
                    (body)
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn get_inventory(
        &self,
        context: &C) -> Result<GetInventoryResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/store/inventory",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        #[allow(clippy::collapsible_match)]
        if let Some(auth_data) = Has::<Option<AuthData>>::get(context).as_ref() {
            #[allow(clippy::single_match, clippy::match_single_binding)]
            match auth_data {
                _ => {}
            }
        }

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                let body = serde_json::from_str::<std::collections::HashMap<String, i32>>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(GetInventoryResponse::SuccessfulOperation
                    (body)
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn place_order(
        &self,
        param_body: models::Order,
        context: &C) -> Result<PlaceOrderResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/store/order",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                // ToDo: this will move to swagger-rs and become a standard From conversion trait
                // once https://github.com/RReverser/serde-xml-rs/pull/45 is accepted upstream
                let body = serde_xml_rs::from_str::<models::Order>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(PlaceOrderResponse::SuccessfulOperation
                    (body)
                )
            }
            400 => {
                Ok(
                    PlaceOrderResponse::InvalidOrder
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn delete_order(
        &self,
        param_order_id: String,
        context: &C) -> Result<DeleteOrderResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/store/order/{order_id}",
            self.base_path
            ,order_id=utf8_percent_encode(&param_order_id.to_string(), ID_ENCODE_SET)
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("DELETE")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            400 => {
                Ok(
                    DeleteOrderResponse::InvalidIDSupplied
                )
            }
            404 => {
                Ok(
                    DeleteOrderResponse::OrderNotFound
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn get_order_by_id(
        &self,
        param_order_id: i64,
        context: &C) -> Result<GetOrderByIdResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/store/order/{order_id}",
            self.base_path
            ,order_id=utf8_percent_encode(&param_order_id.to_string(), ID_ENCODE_SET)
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                // ToDo: this will move to swagger-rs and become a standard From conversion trait
                // once https://github.com/RReverser/serde-xml-rs/pull/45 is accepted upstream
                let body = serde_xml_rs::from_str::<models::Order>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(GetOrderByIdResponse::SuccessfulOperation
                    (body)
                )
            }
            400 => {
                Ok(
                    GetOrderByIdResponse::InvalidIDSupplied
                )
            }
            404 => {
                Ok(
                    GetOrderByIdResponse::OrderNotFound
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn create_user(
        &self,
        param_body: models::User,
        context: &C) -> Result<CreateUserResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/user",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            0 => {
                Ok(
                    CreateUserResponse::SuccessfulOperation
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn create_users_with_array_input(
        &self,
        param_body: &Vec<models::User>,
        context: &C) -> Result<CreateUsersWithArrayInputResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/user/createWithArray",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            0 => {
                Ok(
                    CreateUsersWithArrayInputResponse::SuccessfulOperation
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn create_users_with_list_input(
        &self,
        param_body: &Vec<models::User>,
        context: &C) -> Result<CreateUsersWithListInputResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/user/createWithList",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("POST")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            0 => {
                Ok(
                    CreateUsersWithListInputResponse::SuccessfulOperation
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn login_user(
        &self,
        param_username: String,
        param_password: String,
        context: &C) -> Result<LoginUserResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/user/login",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
                query_string.append_pair("username",
                    &param_username);
                query_string.append_pair("password",
                    &param_password);
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let response_x_rate_limit = match response.headers().get(HeaderName::from_static("x-rate-limit")) {
                    Some(response_x_rate_limit) => {
                        let response_x_rate_limit = response_x_rate_limit.clone();
                        let response_x_rate_limit = match TryInto::<header::IntoHeaderValue<i32>>::try_into(response_x_rate_limit) {
                            Ok(value) => value,
                            Err(e) => {
                                return Err(ApiError(format!("Invalid response header X-Rate-Limit for response 200 - {e}")));
                            },
                        };
                        Some(response_x_rate_limit.0)
                        },
                    None => None,
                };

                let response_x_expires_after = match response.headers().get(HeaderName::from_static("x-expires-after")) {
                    Some(response_x_expires_after) => {
                        let response_x_expires_after = response_x_expires_after.clone();
                        let response_x_expires_after = match TryInto::<header::IntoHeaderValue<chrono::DateTime::<chrono::Utc>>>::try_into(response_x_expires_after) {
                            Ok(value) => value,
                            Err(e) => {
                                return Err(ApiError(format!("Invalid response header X-Expires-After for response 200 - {e}")));
                            },
                        };
                        Some(response_x_expires_after.0)
                        },
                    None => None,
                };

                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                // ToDo: this will move to swagger-rs and become a standard From conversion trait
                // once https://github.com/RReverser/serde-xml-rs/pull/45 is accepted upstream
                let body = serde_xml_rs::from_str::<String>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(LoginUserResponse::SuccessfulOperation
                    {
                        body,
                        x_rate_limit: response_x_rate_limit,
                        x_expires_after: response_x_expires_after,
                    }
                )
            }
            400 => {
                Ok(
                    LoginUserResponse::InvalidUsername
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn logout_user(
        &self,
        context: &C) -> Result<LogoutUserResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/user/logout",
            self.base_path
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            0 => {
                Ok(
                    LogoutUserResponse::SuccessfulOperation
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn delete_user(
        &self,
        param_username: String,
        context: &C) -> Result<DeleteUserResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/user/{username}",
            self.base_path
            ,username=utf8_percent_encode(&param_username.to_string(), ID_ENCODE_SET)
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("DELETE")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            400 => {
                Ok(
                    DeleteUserResponse::InvalidUsernameSupplied
                )
            }
            404 => {
                Ok(
                    DeleteUserResponse::UserNotFound
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn get_user_by_name(
        &self,
        param_username: String,
        context: &C) -> Result<GetUserByNameResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/user/{username}",
            self.base_path
            ,username=utf8_percent_encode(&param_username.to_string(), ID_ENCODE_SET)
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("GET")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            200 => {
                let body = response.into_body();
                let body = body
                        .into_raw()
                        .map_err(|e| ApiError(format!("Failed to read response: {e}"))).await?;

                let body = str::from_utf8(&body)
                    .map_err(|e| ApiError(format!("Response was not valid UTF8: {e}")))?;
                // ToDo: this will move to swagger-rs and become a standard From conversion trait
                // once https://github.com/RReverser/serde-xml-rs/pull/45 is accepted upstream
                let body = serde_xml_rs::from_str::<models::User>(body)
                    .map_err(|e| ApiError(format!("Response body did not match the schema: {e}")))?;

                Ok(GetUserByNameResponse::SuccessfulOperation
                    (body)
                )
            }
            400 => {
                Ok(
                    GetUserByNameResponse::InvalidUsernameSupplied
                )
            }
            404 => {
                Ok(
                    GetUserByNameResponse::UserNotFound
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
    async fn update_user(
        &self,
        param_username: String,
        param_body: models::User,
        context: &C) -> Result<UpdateUserResponse, ApiError>
    {
        let mut client_service = self.client_service.clone();
        #[allow(clippy::uninlined_format_args)]
        let mut uri = format!(
            "{}/v2/user/{username}",
            self.base_path
            ,username=utf8_percent_encode(&param_username.to_string(), ID_ENCODE_SET)
        );

        // Query parameters
        let query_string = {
            let mut query_string = form_urlencoded::Serializer::new("".to_owned());
            query_string.finish()
        };
        if !query_string.is_empty() {
            uri += "?";
            uri += &query_string;
        }

        let uri = match Uri::from_str(&uri) {
            Ok(uri) => uri,
            Err(err) => return Err(ApiError(format!("Unable to build URI: {err}"))),
        };

        let mut request = match Request::builder()
            .method("PUT")
            .uri(uri)
            .body(Body::empty()) {
                Ok(req) => req,
                Err(e) => return Err(ApiError(format!("Unable to create request: {e}")))
        };

        // Consumes basic body
        // Body parameter
        let body = serde_json::to_string(&param_body).expect("impossible to fail to serialize");
        *request.body_mut() = Body::from(body);

        let header = "application/json";
        request.headers_mut().insert(CONTENT_TYPE, match HeaderValue::from_str(header) {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create header: {header} - {e}")))
        });
        let header = HeaderValue::from_str(Has::<XSpanIdString>::get(context).0.as_str());
        request.headers_mut().insert(HeaderName::from_static("x-span-id"), match header {
            Ok(h) => h,
            Err(e) => return Err(ApiError(format!("Unable to create X-Span ID header value: {e}")))
        });

        let response = client_service.call((request, context.clone()))
            .map_err(|e| ApiError(format!("No response received: {e}"))).await?;

        match response.status().as_u16() {
            400 => {
                Ok(
                    UpdateUserResponse::InvalidUserSupplied
                )
            }
            404 => {
                Ok(
                    UpdateUserResponse::UserNotFound
                )
            }
            code => {
                let headers = response.headers().clone();
                let body = response.into_body()
                       .take(100)
                       .into_raw().await;
                Err(ApiError(format!("Unexpected response code {code}:\n{headers:?}\n\n{}",
                    match body {
                        Ok(body) => match String::from_utf8(body) {
                            Ok(body) => body,
                            Err(e) => format!("<Body was not UTF8: {e:?}>"),
                        },
                        Err(e) => format!("<Failed to read body: {e}>"),
                    }
                )))
            }
        }
    }
}

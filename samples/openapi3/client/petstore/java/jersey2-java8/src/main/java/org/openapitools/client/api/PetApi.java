package org.openapitools.client.api;

import org.openapitools.client.ApiException;
import org.openapitools.client.ApiClient;
import org.openapitools.client.ApiResponse;
import org.openapitools.client.Configuration;
import org.openapitools.client.Pair;

import javax.ws.rs.core.GenericType;

import java.io.File;
import org.openapitools.client.model.ModelApiResponse;
import org.openapitools.client.model.Pet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.15.0-SNAPSHOT")
public class PetApi {
  private ApiClient apiClient;

  public PetApi() {
    this(Configuration.getDefaultApiClient());
  }

  public PetApi(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Get the API client
   *
   * @return API client
   */
  public ApiClient getApiClient() {
    return apiClient;
  }

  /**
   * Set the API client
   *
   * @param apiClient an instance of API client
   */
  public void setApiClient(ApiClient apiClient) {
    this.apiClient = apiClient;
  }

  /**
   * Add a new pet to the store
   * 
   * @param pet Pet object that needs to be added to the store (required)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 405 </td><td> Invalid input </td><td>  -  </td></tr>
     </table>
   */
  public void addPet(@javax.annotation.Nonnull Pet pet) throws ApiException {
    addPetWithHttpInfo(pet);
  }

  /**
   * Add a new pet to the store
   * 
   * @param pet Pet object that needs to be added to the store (required)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 405 </td><td> Invalid input </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> addPetWithHttpInfo(@javax.annotation.Nonnull Pet pet) throws ApiException {
    // Check required parameters
    if (pet == null) {
      throw new ApiException(400, "Missing the required parameter 'pet' when calling addPet");
    }

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/json", "application/xml");
    String[] localVarAuthNames = new String[] {"petstore_auth", "http_signature_test"};
    return apiClient.invokeAPI("PetApi.addPet", "/pet", "POST", new ArrayList<>(), pet,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * Deletes a pet
   * 
   * @param petId Pet id to delete (required)
   * @param apiKey  (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 400 </td><td> Invalid pet value </td><td>  -  </td></tr>
     </table>
   */
  public void deletePet(@javax.annotation.Nonnull Long petId, @javax.annotation.Nullable String apiKey) throws ApiException {
    deletePetWithHttpInfo(petId, apiKey);
  }

  /**
   * Deletes a pet
   * 
   * @param petId Pet id to delete (required)
   * @param apiKey  (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 400 </td><td> Invalid pet value </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> deletePetWithHttpInfo(@javax.annotation.Nonnull Long petId, @javax.annotation.Nullable String apiKey) throws ApiException {
    // Check required parameters
    if (petId == null) {
      throw new ApiException(400, "Missing the required parameter 'petId' when calling deletePet");
    }

    // Path parameters
    String localVarPath = "/pet/{petId}"
            .replaceAll("\\{petId}", apiClient.escapeString(petId.toString()));

    // Header parameters
    Map<String, String> localVarHeaderParams = new LinkedHashMap<>();
    if (apiKey != null) {
      localVarHeaderParams.put("api_key", apiClient.parameterToString(apiKey));
    }

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"petstore_auth"};
    return apiClient.invokeAPI("PetApi.deletePet", localVarPath, "DELETE", new ArrayList<>(), null,
                               localVarHeaderParams, new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * Finds Pets by status
   * Multiple status values can be provided with comma separated strings
   * @param status Status values that need to be considered for filter (required)
   * @return List&lt;Pet&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> successful operation </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid status value </td><td>  -  </td></tr>
     </table>
   */
  public List<Pet> findPetsByStatus(@javax.annotation.Nonnull List<String> status) throws ApiException {
    return findPetsByStatusWithHttpInfo(status).getData();
  }

  /**
   * Finds Pets by status
   * Multiple status values can be provided with comma separated strings
   * @param status Status values that need to be considered for filter (required)
   * @return ApiResponse&lt;List&lt;Pet&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> successful operation </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid status value </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<List<Pet>> findPetsByStatusWithHttpInfo(@javax.annotation.Nonnull List<String> status) throws ApiException {
    // Check required parameters
    if (status == null) {
      throw new ApiException(400, "Missing the required parameter 'status' when calling findPetsByStatus");
    }

    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("csv", "status", status)
    );

    String localVarAccept = apiClient.selectHeaderAccept("application/xml", "application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"petstore_auth", "http_signature_test"};
    GenericType<List<Pet>> localVarReturnType = new GenericType<List<Pet>>() {};
    return apiClient.invokeAPI("PetApi.findPetsByStatus", "/pet/findByStatus", "GET", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * Finds Pets by tags
   * Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.
   * @param tags Tags to filter by (required)
   * @return List&lt;Pet&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> successful operation </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid tag value </td><td>  -  </td></tr>
     </table>
   * @deprecated
   */
  @Deprecated
  public List<Pet> findPetsByTags(@javax.annotation.Nonnull List<String> tags) throws ApiException {
    return findPetsByTagsWithHttpInfo(tags).getData();
  }

  /**
   * Finds Pets by tags
   * Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.
   * @param tags Tags to filter by (required)
   * @return ApiResponse&lt;List&lt;Pet&gt;&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> successful operation </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid tag value </td><td>  -  </td></tr>
     </table>
   * @deprecated
   */
  @Deprecated
  public ApiResponse<List<Pet>> findPetsByTagsWithHttpInfo(@javax.annotation.Nonnull List<String> tags) throws ApiException {
    // Check required parameters
    if (tags == null) {
      throw new ApiException(400, "Missing the required parameter 'tags' when calling findPetsByTags");
    }

    // Query parameters
    List<Pair> localVarQueryParams = new ArrayList<>(
            apiClient.parameterToPairs("csv", "tags", tags)
    );

    String localVarAccept = apiClient.selectHeaderAccept("application/xml", "application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"petstore_auth", "http_signature_test"};
    GenericType<List<Pet>> localVarReturnType = new GenericType<List<Pet>>() {};
    return apiClient.invokeAPI("PetApi.findPetsByTags", "/pet/findByTags", "GET", localVarQueryParams, null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * Find pet by ID
   * Returns a single pet
   * @param petId ID of pet to return (required)
   * @return Pet
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> successful operation </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid ID supplied </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Pet not found </td><td>  -  </td></tr>
     </table>
   */
  public Pet getPetById(@javax.annotation.Nonnull Long petId) throws ApiException {
    return getPetByIdWithHttpInfo(petId).getData();
  }

  /**
   * Find pet by ID
   * Returns a single pet
   * @param petId ID of pet to return (required)
   * @return ApiResponse&lt;Pet&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> successful operation </td><td>  -  </td></tr>
       <tr><td> 400 </td><td> Invalid ID supplied </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Pet not found </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Pet> getPetByIdWithHttpInfo(@javax.annotation.Nonnull Long petId) throws ApiException {
    // Check required parameters
    if (petId == null) {
      throw new ApiException(400, "Missing the required parameter 'petId' when calling getPetById");
    }

    // Path parameters
    String localVarPath = "/pet/{petId}"
            .replaceAll("\\{petId}", apiClient.escapeString(petId.toString()));

    String localVarAccept = apiClient.selectHeaderAccept("application/xml", "application/json");
    String localVarContentType = apiClient.selectHeaderContentType();
    String[] localVarAuthNames = new String[] {"api_key"};
    GenericType<Pet> localVarReturnType = new GenericType<Pet>() {};
    return apiClient.invokeAPI("PetApi.getPetById", localVarPath, "GET", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * Update an existing pet
   * 
   * @param pet Pet object that needs to be added to the store (required)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 400 </td><td> Invalid ID supplied </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Pet not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Validation exception </td><td>  -  </td></tr>
     </table>
   */
  public void updatePet(@javax.annotation.Nonnull Pet pet) throws ApiException {
    updatePetWithHttpInfo(pet);
  }

  /**
   * Update an existing pet
   * 
   * @param pet Pet object that needs to be added to the store (required)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 400 </td><td> Invalid ID supplied </td><td>  -  </td></tr>
       <tr><td> 404 </td><td> Pet not found </td><td>  -  </td></tr>
       <tr><td> 405 </td><td> Validation exception </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> updatePetWithHttpInfo(@javax.annotation.Nonnull Pet pet) throws ApiException {
    // Check required parameters
    if (pet == null) {
      throw new ApiException(400, "Missing the required parameter 'pet' when calling updatePet");
    }

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/json", "application/xml");
    String[] localVarAuthNames = new String[] {"petstore_auth", "http_signature_test"};
    return apiClient.invokeAPI("PetApi.updatePet", "/pet", "PUT", new ArrayList<>(), pet,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), new LinkedHashMap<>(), localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * Updates a pet in the store with form data
   * 
   * @param petId ID of pet that needs to be updated (required)
   * @param name Updated name of the pet (optional)
   * @param status Updated status of the pet (optional)
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 405 </td><td> Invalid input </td><td>  -  </td></tr>
     </table>
   */
  public void updatePetWithForm(@javax.annotation.Nonnull Long petId, @javax.annotation.Nullable String name, @javax.annotation.Nullable String status) throws ApiException {
    updatePetWithFormWithHttpInfo(petId, name, status);
  }

  /**
   * Updates a pet in the store with form data
   * 
   * @param petId ID of pet that needs to be updated (required)
   * @param name Updated name of the pet (optional)
   * @param status Updated status of the pet (optional)
   * @return ApiResponse&lt;Void&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 405 </td><td> Invalid input </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<Void> updatePetWithFormWithHttpInfo(@javax.annotation.Nonnull Long petId, @javax.annotation.Nullable String name, @javax.annotation.Nullable String status) throws ApiException {
    // Check required parameters
    if (petId == null) {
      throw new ApiException(400, "Missing the required parameter 'petId' when calling updatePetWithForm");
    }

    // Path parameters
    String localVarPath = "/pet/{petId}"
            .replaceAll("\\{petId}", apiClient.escapeString(petId.toString()));

    // Form parameters
    Map<String, Object> localVarFormParams = new LinkedHashMap<>();
    if (name != null) {
      localVarFormParams.put("name", name);
    }
    if (status != null) {
      localVarFormParams.put("status", status);
    }

    String localVarAccept = apiClient.selectHeaderAccept();
    String localVarContentType = apiClient.selectHeaderContentType("application/x-www-form-urlencoded");
    String[] localVarAuthNames = new String[] {"petstore_auth"};
    return apiClient.invokeAPI("PetApi.updatePetWithForm", localVarPath, "POST", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), localVarFormParams, localVarAccept, localVarContentType,
                               localVarAuthNames, null, false);
  }
  /**
   * uploads an image
   * 
   * @param petId ID of pet to update (required)
   * @param additionalMetadata Additional data to pass to server (optional)
   * @param _file file to upload (optional)
   * @return ModelApiResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> successful operation </td><td>  -  </td></tr>
     </table>
   */
  public ModelApiResponse uploadFile(@javax.annotation.Nonnull Long petId, @javax.annotation.Nullable String additionalMetadata, @javax.annotation.Nullable File _file) throws ApiException {
    return uploadFileWithHttpInfo(petId, additionalMetadata, _file).getData();
  }

  /**
   * uploads an image
   * 
   * @param petId ID of pet to update (required)
   * @param additionalMetadata Additional data to pass to server (optional)
   * @param _file file to upload (optional)
   * @return ApiResponse&lt;ModelApiResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> successful operation </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ModelApiResponse> uploadFileWithHttpInfo(@javax.annotation.Nonnull Long petId, @javax.annotation.Nullable String additionalMetadata, @javax.annotation.Nullable File _file) throws ApiException {
    // Check required parameters
    if (petId == null) {
      throw new ApiException(400, "Missing the required parameter 'petId' when calling uploadFile");
    }

    // Path parameters
    String localVarPath = "/pet/{petId}/uploadImage"
            .replaceAll("\\{petId}", apiClient.escapeString(petId.toString()));

    // Form parameters
    Map<String, Object> localVarFormParams = new LinkedHashMap<>();
    if (additionalMetadata != null) {
      localVarFormParams.put("additionalMetadata", additionalMetadata);
    }
    if (_file != null) {
      localVarFormParams.put("file", _file);
    }

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("multipart/form-data");
    String[] localVarAuthNames = new String[] {"petstore_auth"};
    GenericType<ModelApiResponse> localVarReturnType = new GenericType<ModelApiResponse>() {};
    return apiClient.invokeAPI("PetApi.uploadFile", localVarPath, "POST", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), localVarFormParams, localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
  /**
   * uploads an image (required)
   * 
   * @param petId ID of pet to update (required)
   * @param requiredFile file to upload (required)
   * @param additionalMetadata Additional data to pass to server (optional)
   * @return ModelApiResponse
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> successful operation </td><td>  -  </td></tr>
     </table>
   */
  public ModelApiResponse uploadFileWithRequiredFile(@javax.annotation.Nonnull Long petId, @javax.annotation.Nonnull File requiredFile, @javax.annotation.Nullable String additionalMetadata) throws ApiException {
    return uploadFileWithRequiredFileWithHttpInfo(petId, requiredFile, additionalMetadata).getData();
  }

  /**
   * uploads an image (required)
   * 
   * @param petId ID of pet to update (required)
   * @param requiredFile file to upload (required)
   * @param additionalMetadata Additional data to pass to server (optional)
   * @return ApiResponse&lt;ModelApiResponse&gt;
   * @throws ApiException if fails to make API call
   * @http.response.details
     <table border="1">
       <caption>Response Details</caption>
       <tr><td> Status Code </td><td> Description </td><td> Response Headers </td></tr>
       <tr><td> 200 </td><td> successful operation </td><td>  -  </td></tr>
     </table>
   */
  public ApiResponse<ModelApiResponse> uploadFileWithRequiredFileWithHttpInfo(@javax.annotation.Nonnull Long petId, @javax.annotation.Nonnull File requiredFile, @javax.annotation.Nullable String additionalMetadata) throws ApiException {
    // Check required parameters
    if (petId == null) {
      throw new ApiException(400, "Missing the required parameter 'petId' when calling uploadFileWithRequiredFile");
    }
    if (requiredFile == null) {
      throw new ApiException(400, "Missing the required parameter 'requiredFile' when calling uploadFileWithRequiredFile");
    }

    // Path parameters
    String localVarPath = "/fake/{petId}/uploadImageWithRequiredFile"
            .replaceAll("\\{petId}", apiClient.escapeString(petId.toString()));

    // Form parameters
    Map<String, Object> localVarFormParams = new LinkedHashMap<>();
    if (additionalMetadata != null) {
      localVarFormParams.put("additionalMetadata", additionalMetadata);
    }
    localVarFormParams.put("requiredFile", requiredFile);

    String localVarAccept = apiClient.selectHeaderAccept("application/json");
    String localVarContentType = apiClient.selectHeaderContentType("multipart/form-data");
    String[] localVarAuthNames = new String[] {"petstore_auth"};
    GenericType<ModelApiResponse> localVarReturnType = new GenericType<ModelApiResponse>() {};
    return apiClient.invokeAPI("PetApi.uploadFileWithRequiredFile", localVarPath, "POST", new ArrayList<>(), null,
                               new LinkedHashMap<>(), new LinkedHashMap<>(), localVarFormParams, localVarAccept, localVarContentType,
                               localVarAuthNames, localVarReturnType, false);
  }
}

package org.openapitools.client.api;

import org.openapitools.client.ApiClient;
import org.openapitools.client.EncodingUtils;
import org.openapitools.client.model.ApiResponse;

import java.io.File;
import org.openapitools.client.model.ModelApiResponse;
import org.openapitools.client.model.Pet;
import java.util.Set;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import feign.*;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.15.0-SNAPSHOT")
public interface PetApi extends ApiClient.Api {


  /**
   * Add a new pet to the store
   * 
   * @param body Pet object that needs to be added to the store (required)
   */
  @RequestLine("POST /pet")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  void addPet(@javax.annotation.Nonnull Pet body);

  /**
   * Add a new pet to the store
   * Similar to <code>addPet</code> but it also returns the http response headers .
   * 
   * @param body Pet object that needs to be added to the store (required)
   */
  @RequestLine("POST /pet")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<Void> addPetWithHttpInfo(@javax.annotation.Nonnull Pet body);



  /**
   * Deletes a pet
   * 
   * @param petId Pet id to delete (required)
   * @param apiKey  (optional)
   */
  @RequestLine("DELETE /pet/{petId}")
  @Headers({
    "Accept: application/json",
    "api_key: {apiKey}"
  })
  void deletePet(@Param("petId") @javax.annotation.Nonnull Long petId, @Param("apiKey") @javax.annotation.Nullable String apiKey);

  /**
   * Deletes a pet
   * Similar to <code>deletePet</code> but it also returns the http response headers .
   * 
   * @param petId Pet id to delete (required)
   * @param apiKey  (optional)
   */
  @RequestLine("DELETE /pet/{petId}")
  @Headers({
    "Accept: application/json",
    "api_key: {apiKey}"
  })
  ApiResponse<Void> deletePetWithHttpInfo(@Param("petId") @javax.annotation.Nonnull Long petId, @Param("apiKey") @javax.annotation.Nullable String apiKey);



  /**
   * Finds Pets by status
   * Multiple status values can be provided with comma separated strings
   * @param status Status values that need to be considered for filter (required)
   * @return List&lt;Pet&gt;
   */
  @RequestLine("GET /pet/findByStatus?status={status}")
  @Headers({
    "Accept: application/json,application/xml",
  })
  List<Pet> findPetsByStatus(@Param("status") @javax.annotation.Nonnull List<String> status);

  /**
   * Finds Pets by status
   * Similar to <code>findPetsByStatus</code> but it also returns the http response headers .
   * Multiple status values can be provided with comma separated strings
   * @param status Status values that need to be considered for filter (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /pet/findByStatus?status={status}")
  @Headers({
    "Accept: application/json,application/xml",
  })
  ApiResponse<List<Pet>> findPetsByStatusWithHttpInfo(@Param("status") @javax.annotation.Nonnull List<String> status);


  /**
   * Finds Pets by status
   * Multiple status values can be provided with comma separated strings
   * Note, this is equivalent to the other <code>findPetsByStatus</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link FindPetsByStatusQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>status - Status values that need to be considered for filter (required)</li>
   *   </ul>
   * @return List&lt;Pet&gt;
   */
  @RequestLine("GET /pet/findByStatus?status={status}")
  @Headers({
  "Accept: application/json,application/xml",
  })
  List<Pet> findPetsByStatus(@QueryMap(encoded=true) FindPetsByStatusQueryParams queryParams);

  /**
  * Finds Pets by status
  * Multiple status values can be provided with comma separated strings
  * Note, this is equivalent to the other <code>findPetsByStatus</code> that receives the query parameters as a map,
  * but this one also exposes the Http response headers
      * @param queryParams Map of query parameters as name-value pairs
      *   <p>The following elements may be specified in the query map:</p>
      *   <ul>
          *   <li>status - Status values that need to be considered for filter (required)</li>
      *   </ul>
          * @return List&lt;Pet&gt;
      */
      @RequestLine("GET /pet/findByStatus?status={status}")
      @Headers({
    "Accept: application/json,application/xml",
      })
   ApiResponse<List<Pet>> findPetsByStatusWithHttpInfo(@QueryMap(encoded=true) FindPetsByStatusQueryParams queryParams);


   /**
   * A convenience class for generating query parameters for the
   * <code>findPetsByStatus</code> method in a fluent style.
   */
  public static class FindPetsByStatusQueryParams extends HashMap<String, Object> {
    public FindPetsByStatusQueryParams status(@javax.annotation.Nonnull final List<String> value) {
      put("status", EncodingUtils.encodeCollection(value, "csv"));
      return this;
    }
  }

  /**
   * Finds Pets by tags
   * Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.
   * @param tags Tags to filter by (required)
   * @return Set&lt;Pet&gt;
   * @deprecated
   */
  @Deprecated
  @RequestLine("GET /pet/findByTags?tags={tags}")
  @Headers({
    "Accept: application/json,application/xml",
  })
  Set<Pet> findPetsByTags(@Param("tags") @javax.annotation.Nonnull Set<String> tags);

  /**
   * Finds Pets by tags
   * Similar to <code>findPetsByTags</code> but it also returns the http response headers .
   * Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.
   * @param tags Tags to filter by (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   * @deprecated
   */
  @Deprecated
  @RequestLine("GET /pet/findByTags?tags={tags}")
  @Headers({
    "Accept: application/json,application/xml",
  })
  ApiResponse<Set<Pet>> findPetsByTagsWithHttpInfo(@Param("tags") @javax.annotation.Nonnull Set<String> tags);


  /**
   * Finds Pets by tags
   * Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.
   * Note, this is equivalent to the other <code>findPetsByTags</code> method,
   * but with the query parameters collected into a single Map parameter. This
   * is convenient for services with optional query parameters, especially when
   * used with the {@link FindPetsByTagsQueryParams} class that allows for
   * building up this map in a fluent style.
   * @param queryParams Map of query parameters as name-value pairs
   *   <p>The following elements may be specified in the query map:</p>
   *   <ul>
   *   <li>tags - Tags to filter by (required)</li>
   *   </ul>
   * @return Set&lt;Pet&gt;
   * @deprecated
   */
  @Deprecated
  @RequestLine("GET /pet/findByTags?tags={tags}")
  @Headers({
  "Accept: application/json,application/xml",
  })
  Set<Pet> findPetsByTags(@QueryMap(encoded=true) FindPetsByTagsQueryParams queryParams);

  /**
  * Finds Pets by tags
  * Multiple tags can be provided with comma separated strings. Use tag1, tag2, tag3 for testing.
  * Note, this is equivalent to the other <code>findPetsByTags</code> that receives the query parameters as a map,
  * but this one also exposes the Http response headers
      * @param queryParams Map of query parameters as name-value pairs
      *   <p>The following elements may be specified in the query map:</p>
      *   <ul>
          *   <li>tags - Tags to filter by (required)</li>
      *   </ul>
          * @return Set&lt;Pet&gt;
          * @deprecated
      */
          @Deprecated
      @RequestLine("GET /pet/findByTags?tags={tags}")
      @Headers({
    "Accept: application/json,application/xml",
      })
   ApiResponse<Set<Pet>> findPetsByTagsWithHttpInfo(@QueryMap(encoded=true) FindPetsByTagsQueryParams queryParams);


   /**
   * A convenience class for generating query parameters for the
   * <code>findPetsByTags</code> method in a fluent style.
   */
  public static class FindPetsByTagsQueryParams extends HashMap<String, Object> {
    public FindPetsByTagsQueryParams tags(@javax.annotation.Nonnull final Set<String> value) {
      put("tags", EncodingUtils.encodeCollection(value, "csv"));
      return this;
    }
  }

  /**
   * Find pet by ID
   * Returns a single pet
   * @param petId ID of pet to return (required)
   * @return Pet
   */
  @RequestLine("GET /pet/{petId}")
  @Headers({
    "Accept: application/json,application/xml",
  })
  Pet getPetById(@Param("petId") @javax.annotation.Nonnull Long petId);

  /**
   * Find pet by ID
   * Similar to <code>getPetById</code> but it also returns the http response headers .
   * Returns a single pet
   * @param petId ID of pet to return (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /pet/{petId}")
  @Headers({
    "Accept: application/json,application/xml",
  })
  ApiResponse<Pet> getPetByIdWithHttpInfo(@Param("petId") @javax.annotation.Nonnull Long petId);



  /**
   * Update an existing pet
   * 
   * @param body Pet object that needs to be added to the store (required)
   */
  @RequestLine("PUT /pet")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  void updatePet(@javax.annotation.Nonnull Pet body);

  /**
   * Update an existing pet
   * Similar to <code>updatePet</code> but it also returns the http response headers .
   * 
   * @param body Pet object that needs to be added to the store (required)
   */
  @RequestLine("PUT /pet")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<Void> updatePetWithHttpInfo(@javax.annotation.Nonnull Pet body);



  /**
   * Updates a pet in the store with form data
   * 
   * @param petId ID of pet that needs to be updated (required)
   * @param name Updated name of the pet (optional)
   * @param status Updated status of the pet (optional)
   */
  @RequestLine("POST /pet/{petId}")
  @Headers({
    "Content-Type: application/x-www-form-urlencoded",
    "Accept: application/json",
  })
  void updatePetWithForm(@Param("petId") @javax.annotation.Nonnull Long petId, @Param("name") @javax.annotation.Nullable String name, @Param("status") @javax.annotation.Nullable String status);

  /**
   * Updates a pet in the store with form data
   * Similar to <code>updatePetWithForm</code> but it also returns the http response headers .
   * 
   * @param petId ID of pet that needs to be updated (required)
   * @param name Updated name of the pet (optional)
   * @param status Updated status of the pet (optional)
   */
  @RequestLine("POST /pet/{petId}")
  @Headers({
    "Content-Type: application/x-www-form-urlencoded",
    "Accept: application/json",
  })
  ApiResponse<Void> updatePetWithFormWithHttpInfo(@Param("petId") @javax.annotation.Nonnull Long petId, @Param("name") @javax.annotation.Nullable String name, @Param("status") @javax.annotation.Nullable String status);



  /**
   * uploads an image
   * 
   * @param petId ID of pet to update (required)
   * @param additionalMetadata Additional data to pass to server (optional)
   * @param file file to upload (optional)
   * @return ModelApiResponse
   */
  @RequestLine("POST /pet/{petId}/uploadImage")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
  })
  ModelApiResponse uploadFile(@Param("petId") @javax.annotation.Nonnull Long petId, @Param("additionalMetadata") @javax.annotation.Nullable String additionalMetadata, @Param("file") @javax.annotation.Nullable File file);

  /**
   * uploads an image
   * Similar to <code>uploadFile</code> but it also returns the http response headers .
   * 
   * @param petId ID of pet to update (required)
   * @param additionalMetadata Additional data to pass to server (optional)
   * @param file file to upload (optional)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /pet/{petId}/uploadImage")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
  })
  ApiResponse<ModelApiResponse> uploadFileWithHttpInfo(@Param("petId") @javax.annotation.Nonnull Long petId, @Param("additionalMetadata") @javax.annotation.Nullable String additionalMetadata, @Param("file") @javax.annotation.Nullable File file);



  /**
   * uploads an image (required)
   * 
   * @param petId ID of pet to update (required)
   * @param requiredFile file to upload (required)
   * @param additionalMetadata Additional data to pass to server (optional)
   * @return ModelApiResponse
   */
  @RequestLine("POST /fake/{petId}/uploadImageWithRequiredFile")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
  })
  ModelApiResponse uploadFileWithRequiredFile(@Param("petId") @javax.annotation.Nonnull Long petId, @Param("requiredFile") @javax.annotation.Nonnull File requiredFile, @Param("additionalMetadata") @javax.annotation.Nullable String additionalMetadata);

  /**
   * uploads an image (required)
   * Similar to <code>uploadFileWithRequiredFile</code> but it also returns the http response headers .
   * 
   * @param petId ID of pet to update (required)
   * @param requiredFile file to upload (required)
   * @param additionalMetadata Additional data to pass to server (optional)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /fake/{petId}/uploadImageWithRequiredFile")
  @Headers({
    "Content-Type: multipart/form-data",
    "Accept: application/json",
  })
  ApiResponse<ModelApiResponse> uploadFileWithRequiredFileWithHttpInfo(@Param("petId") @javax.annotation.Nonnull Long petId, @Param("requiredFile") @javax.annotation.Nonnull File requiredFile, @Param("additionalMetadata") @javax.annotation.Nullable String additionalMetadata);


}

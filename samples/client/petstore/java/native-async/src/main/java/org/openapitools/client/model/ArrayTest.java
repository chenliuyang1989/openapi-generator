/*
 * OpenAPI Petstore
 * This spec is mainly for testing Petstore server and contains fake endpoints, models. Please do not use this for any other purpose. Special characters: \" \\
 *
 * The version of the OpenAPI document: 1.0.0
 * 
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package org.openapitools.client.model;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;
import java.util.Objects;
import java.util.Map;
import java.util.HashMap;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.client.model.ReadOnlyFirst;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;


import org.openapitools.client.ApiClient;
/**
 * ArrayTest
 */
@JsonPropertyOrder({
  ArrayTest.JSON_PROPERTY_ARRAY_OF_STRING,
  ArrayTest.JSON_PROPERTY_ARRAY_ARRAY_OF_INTEGER,
  ArrayTest.JSON_PROPERTY_ARRAY_ARRAY_OF_MODEL
})
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", comments = "Generator version: 7.15.0-SNAPSHOT")
public class ArrayTest {
  public static final String JSON_PROPERTY_ARRAY_OF_STRING = "array_of_string";
  @javax.annotation.Nullable
  private List<String> arrayOfString = new ArrayList<>();

  public static final String JSON_PROPERTY_ARRAY_ARRAY_OF_INTEGER = "array_array_of_integer";
  @javax.annotation.Nullable
  private List<List<Long>> arrayArrayOfInteger = new ArrayList<>();

  public static final String JSON_PROPERTY_ARRAY_ARRAY_OF_MODEL = "array_array_of_model";
  @javax.annotation.Nullable
  private List<List<ReadOnlyFirst>> arrayArrayOfModel = new ArrayList<>();

  public ArrayTest() { 
  }

  public ArrayTest arrayOfString(@javax.annotation.Nullable List<String> arrayOfString) {
    this.arrayOfString = arrayOfString;
    return this;
  }

  public ArrayTest addArrayOfStringItem(String arrayOfStringItem) {
    if (this.arrayOfString == null) {
      this.arrayOfString = new ArrayList<>();
    }
    this.arrayOfString.add(arrayOfStringItem);
    return this;
  }

  /**
   * Get arrayOfString
   * @return arrayOfString
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ARRAY_OF_STRING)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public List<String> getArrayOfString() {
    return arrayOfString;
  }


  @JsonProperty(JSON_PROPERTY_ARRAY_OF_STRING)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setArrayOfString(@javax.annotation.Nullable List<String> arrayOfString) {
    this.arrayOfString = arrayOfString;
  }


  public ArrayTest arrayArrayOfInteger(@javax.annotation.Nullable List<List<Long>> arrayArrayOfInteger) {
    this.arrayArrayOfInteger = arrayArrayOfInteger;
    return this;
  }

  public ArrayTest addArrayArrayOfIntegerItem(List<Long> arrayArrayOfIntegerItem) {
    if (this.arrayArrayOfInteger == null) {
      this.arrayArrayOfInteger = new ArrayList<>();
    }
    this.arrayArrayOfInteger.add(arrayArrayOfIntegerItem);
    return this;
  }

  /**
   * Get arrayArrayOfInteger
   * @return arrayArrayOfInteger
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ARRAY_ARRAY_OF_INTEGER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public List<List<Long>> getArrayArrayOfInteger() {
    return arrayArrayOfInteger;
  }


  @JsonProperty(JSON_PROPERTY_ARRAY_ARRAY_OF_INTEGER)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setArrayArrayOfInteger(@javax.annotation.Nullable List<List<Long>> arrayArrayOfInteger) {
    this.arrayArrayOfInteger = arrayArrayOfInteger;
  }


  public ArrayTest arrayArrayOfModel(@javax.annotation.Nullable List<List<ReadOnlyFirst>> arrayArrayOfModel) {
    this.arrayArrayOfModel = arrayArrayOfModel;
    return this;
  }

  public ArrayTest addArrayArrayOfModelItem(List<ReadOnlyFirst> arrayArrayOfModelItem) {
    if (this.arrayArrayOfModel == null) {
      this.arrayArrayOfModel = new ArrayList<>();
    }
    this.arrayArrayOfModel.add(arrayArrayOfModelItem);
    return this;
  }

  /**
   * Get arrayArrayOfModel
   * @return arrayArrayOfModel
   */
  @javax.annotation.Nullable
  @JsonProperty(JSON_PROPERTY_ARRAY_ARRAY_OF_MODEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public List<List<ReadOnlyFirst>> getArrayArrayOfModel() {
    return arrayArrayOfModel;
  }


  @JsonProperty(JSON_PROPERTY_ARRAY_ARRAY_OF_MODEL)
  @JsonInclude(value = JsonInclude.Include.USE_DEFAULTS)
  public void setArrayArrayOfModel(@javax.annotation.Nullable List<List<ReadOnlyFirst>> arrayArrayOfModel) {
    this.arrayArrayOfModel = arrayArrayOfModel;
  }


  /**
   * Return true if this ArrayTest object is equal to o.
   */
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ArrayTest arrayTest = (ArrayTest) o;
    return Objects.equals(this.arrayOfString, arrayTest.arrayOfString) &&
        Objects.equals(this.arrayArrayOfInteger, arrayTest.arrayArrayOfInteger) &&
        Objects.equals(this.arrayArrayOfModel, arrayTest.arrayArrayOfModel);
  }

  @Override
  public int hashCode() {
    return Objects.hash(arrayOfString, arrayArrayOfInteger, arrayArrayOfModel);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ArrayTest {\n");
    sb.append("    arrayOfString: ").append(toIndentedString(arrayOfString)).append("\n");
    sb.append("    arrayArrayOfInteger: ").append(toIndentedString(arrayArrayOfInteger)).append("\n");
    sb.append("    arrayArrayOfModel: ").append(toIndentedString(arrayArrayOfModel)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

  /**
   * Convert the instance into URL query string.
   *
   * @return URL query string
   */
  public String toUrlQueryString() {
    return toUrlQueryString(null);
  }

  /**
   * Convert the instance into URL query string.
   *
   * @param prefix prefix of the query string
   * @return URL query string
   */
  public String toUrlQueryString(String prefix) {
    String suffix = "";
    String containerSuffix = "";
    String containerPrefix = "";
    if (prefix == null) {
      // style=form, explode=true, e.g. /pet?name=cat&type=manx
      prefix = "";
    } else {
      // deepObject style e.g. /pet?id[name]=cat&id[type]=manx
      prefix = prefix + "[";
      suffix = "]";
      containerSuffix = "]";
      containerPrefix = "[";
    }

    StringJoiner joiner = new StringJoiner("&");

    // add `array_of_string` to the URL query string
    if (getArrayOfString() != null) {
      for (int i = 0; i < getArrayOfString().size(); i++) {
        joiner.add(String.format("%sarray_of_string%s%s=%s", prefix, suffix,
            "".equals(suffix) ? "" : String.format("%s%d%s", containerPrefix, i, containerSuffix),
            ApiClient.urlEncode(ApiClient.valueToString(getArrayOfString().get(i)))));
      }
    }

    // add `array_array_of_integer` to the URL query string
    if (getArrayArrayOfInteger() != null) {
      for (int i = 0; i < getArrayArrayOfInteger().size(); i++) {
        joiner.add(String.format("%sarray_array_of_integer%s%s=%s", prefix, suffix,
            "".equals(suffix) ? "" : String.format("%s%d%s", containerPrefix, i, containerSuffix),
            ApiClient.urlEncode(ApiClient.valueToString(getArrayArrayOfInteger().get(i)))));
      }
    }

    // add `array_array_of_model` to the URL query string
    if (getArrayArrayOfModel() != null) {
      for (int i = 0; i < getArrayArrayOfModel().size(); i++) {
        if (getArrayArrayOfModel().get(i) != null) {
          joiner.add(String.format("%sarray_array_of_model%s%s=%s", prefix, suffix,
              "".equals(suffix) ? "" : String.format("%s%d%s", containerPrefix, i, containerSuffix),
              ApiClient.urlEncode(ApiClient.valueToString(getArrayArrayOfModel().get(i)))));
        }
      }
    }

    return joiner.toString();
  }
}


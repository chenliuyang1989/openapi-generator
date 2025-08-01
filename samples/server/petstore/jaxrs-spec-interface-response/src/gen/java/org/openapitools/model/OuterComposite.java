package org.openapitools.model;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;
import java.io.Serializable;
import javax.validation.constraints.*;
import javax.validation.Valid;

import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonTypeName;



@JsonTypeName("OuterComposite")
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", comments = "Generator version: 7.15.0-SNAPSHOT")
public class OuterComposite  implements Serializable {
  private BigDecimal myNumber;
  private String myString;
  private Boolean myBoolean;

  public OuterComposite() {
  }

  /**
   **/
  public OuterComposite myNumber(BigDecimal myNumber) {
    this.myNumber = myNumber;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("my_number")
  @Valid public BigDecimal getMyNumber() {
    return myNumber;
  }

  @JsonProperty("my_number")
  public void setMyNumber(BigDecimal myNumber) {
    this.myNumber = myNumber;
  }

  /**
   **/
  public OuterComposite myString(String myString) {
    this.myString = myString;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("my_string")
  public String getMyString() {
    return myString;
  }

  @JsonProperty("my_string")
  public void setMyString(String myString) {
    this.myString = myString;
  }

  /**
   **/
  public OuterComposite myBoolean(Boolean myBoolean) {
    this.myBoolean = myBoolean;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("my_boolean")
  public Boolean getMyBoolean() {
    return myBoolean;
  }

  @JsonProperty("my_boolean")
  public void setMyBoolean(Boolean myBoolean) {
    this.myBoolean = myBoolean;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OuterComposite outerComposite = (OuterComposite) o;
    return Objects.equals(this.myNumber, outerComposite.myNumber) &&
        Objects.equals(this.myString, outerComposite.myString) &&
        Objects.equals(this.myBoolean, outerComposite.myBoolean);
  }

  @Override
  public int hashCode() {
    return Objects.hash(myNumber, myString, myBoolean);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OuterComposite {\n");
    
    sb.append("    myNumber: ").append(toIndentedString(myNumber)).append("\n");
    sb.append("    myString: ").append(toIndentedString(myString)).append("\n");
    sb.append("    myBoolean: ").append(toIndentedString(myBoolean)).append("\n");
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


}


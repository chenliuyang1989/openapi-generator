package org.openapitools.model;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.File;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import org.joda.time.LocalDate;
import java.io.Serializable;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;

import io.swagger.annotations.*;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.JsonTypeName;



@JsonTypeName("format_test")
@jakarta.annotation.Generated(value = "org.openapitools.codegen.languages.JavaJAXRSSpecServerCodegen", comments = "Generator version: 7.15.0-SNAPSHOT")
public class FormatTest  implements Serializable {
  private Integer integer;
  private Integer int32;
  private Long int64;
  private BigDecimal number;
  private Float _float;
  private Double _double;
  private String string;
  private byte[] _byte;
  private File binary;
  private LocalDate date;
  private Date dateTime;
  private UUID uuid;
  private String password;
  private BigDecimal bigDecimal;

  protected FormatTest(FormatTestBuilder<?, ?> b) {
    this.integer = b.integer;
    this.int32 = b.int32;
    this.int64 = b.int64;
    this.number = b.number;
    this._float = b._float;
    this._double = b._double;
    this.string = b.string;
    this._byte = b._byte;
    this.binary = b.binary;
    this.date = b.date;
    this.dateTime = b.dateTime;
    this.uuid = b.uuid;
    this.password = b.password;
    this.bigDecimal = b.bigDecimal;
  }

  public FormatTest() {
  }

  @JsonCreator
  public FormatTest(
    @JsonProperty(required = true, value = "number") BigDecimal number,
    @JsonProperty(required = true, value = "byte") byte[] _byte,
    @JsonProperty(required = true, value = "date") LocalDate date,
    @JsonProperty(required = true, value = "password") String password
  ) {
    this.number = number;
    this._byte = _byte;
    this.date = date;
    this.password = password;
  }

  /**
   * minimum: 10
   * maximum: 100
   **/
  public FormatTest integer(Integer integer) {
    this.integer = integer;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("integer")
   @Min(10) @Max(100)public Integer getInteger() {
    return integer;
  }

  @JsonProperty("integer")
  public void setInteger(Integer integer) {
    this.integer = integer;
  }

  /**
   * minimum: 20
   * maximum: 200
   **/
  public FormatTest int32(Integer int32) {
    this.int32 = int32;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("int32")
   @Min(20) @Max(200)public Integer getInt32() {
    return int32;
  }

  @JsonProperty("int32")
  public void setInt32(Integer int32) {
    this.int32 = int32;
  }

  /**
   **/
  public FormatTest int64(Long int64) {
    this.int64 = int64;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("int64")
  public Long getInt64() {
    return int64;
  }

  @JsonProperty("int64")
  public void setInt64(Long int64) {
    this.int64 = int64;
  }

  /**
   * minimum: 32.1
   * maximum: 543.2
   **/
  public FormatTest number(BigDecimal number) {
    this.number = number;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty(required = true, value = "number")
  @NotNull @Valid  @DecimalMin("32.1") @DecimalMax("543.2")public BigDecimal getNumber() {
    return number;
  }

  @JsonProperty(required = true, value = "number")
  public void setNumber(BigDecimal number) {
    this.number = number;
  }

  /**
   * minimum: 54.3
   * maximum: 987.6
   **/
  public FormatTest _float(Float _float) {
    this._float = _float;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("float")
   @DecimalMin("54.3") @DecimalMax("987.6")public Float getFloat() {
    return _float;
  }

  @JsonProperty("float")
  public void setFloat(Float _float) {
    this._float = _float;
  }

  /**
   * minimum: 67.8
   * maximum: 123.4
   **/
  public FormatTest _double(Double _double) {
    this._double = _double;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("double")
   @DecimalMin("67.8") @DecimalMax("123.4")public Double getDouble() {
    return _double;
  }

  @JsonProperty("double")
  public void setDouble(Double _double) {
    this._double = _double;
  }

  /**
   **/
  public FormatTest string(String string) {
    this.string = string;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("string")
   @Pattern(regexp="/[a-z]/i")public String getString() {
    return string;
  }

  @JsonProperty("string")
  public void setString(String string) {
    this.string = string;
  }

  /**
   **/
  public FormatTest _byte(byte[] _byte) {
    this._byte = _byte;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty(required = true, value = "byte")
  @NotNull  @Pattern(regexp="^(?:[A-Za-z0-9+/]{4})*(?:[A-Za-z0-9+/]{2}==|[A-Za-z0-9+/]{3}=)?$")public byte[] getByte() {
    return _byte;
  }

  @JsonProperty(required = true, value = "byte")
  public void setByte(byte[] _byte) {
    this._byte = _byte;
  }

  /**
   **/
  public FormatTest binary(File binary) {
    this.binary = binary;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("binary")
  public File getBinary() {
    return binary;
  }

  @JsonProperty("binary")
  public void setBinary(File binary) {
    this.binary = binary;
  }

  /**
   **/
  public FormatTest date(LocalDate date) {
    this.date = date;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty(required = true, value = "date")
  @NotNull public LocalDate getDate() {
    return date;
  }

  @JsonProperty(required = true, value = "date")
  public void setDate(LocalDate date) {
    this.date = date;
  }

  /**
   **/
  public FormatTest dateTime(Date dateTime) {
    this.dateTime = dateTime;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("dateTime")
  public Date getDateTime() {
    return dateTime;
  }

  @JsonProperty("dateTime")
  public void setDateTime(Date dateTime) {
    this.dateTime = dateTime;
  }

  /**
   **/
  public FormatTest uuid(UUID uuid) {
    this.uuid = uuid;
    return this;
  }

  
  @ApiModelProperty(example = "72f98069-206d-4f12-9f12-3d1e525a8e84", value = "")
  @JsonProperty("uuid")
  public UUID getUuid() {
    return uuid;
  }

  @JsonProperty("uuid")
  public void setUuid(UUID uuid) {
    this.uuid = uuid;
  }

  /**
   **/
  public FormatTest password(String password) {
    this.password = password;
    return this;
  }

  
  @ApiModelProperty(required = true, value = "")
  @JsonProperty(required = true, value = "password")
  @NotNull  @Size(min=10,max=64)public String getPassword() {
    return password;
  }

  @JsonProperty(required = true, value = "password")
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   **/
  public FormatTest bigDecimal(BigDecimal bigDecimal) {
    this.bigDecimal = bigDecimal;
    return this;
  }

  
  @ApiModelProperty(value = "")
  @JsonProperty("BigDecimal")
  @Valid public BigDecimal getBigDecimal() {
    return bigDecimal;
  }

  @JsonProperty("BigDecimal")
  public void setBigDecimal(BigDecimal bigDecimal) {
    this.bigDecimal = bigDecimal;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    FormatTest formatTest = (FormatTest) o;
    return Objects.equals(this.integer, formatTest.integer) &&
        Objects.equals(this.int32, formatTest.int32) &&
        Objects.equals(this.int64, formatTest.int64) &&
        Objects.equals(this.number, formatTest.number) &&
        Objects.equals(this._float, formatTest._float) &&
        Objects.equals(this._double, formatTest._double) &&
        Objects.equals(this.string, formatTest.string) &&
        Arrays.equals(this._byte, formatTest._byte) &&
        Objects.equals(this.binary, formatTest.binary) &&
        Objects.equals(this.date, formatTest.date) &&
        Objects.equals(this.dateTime, formatTest.dateTime) &&
        Objects.equals(this.uuid, formatTest.uuid) &&
        Objects.equals(this.password, formatTest.password) &&
        Objects.equals(this.bigDecimal, formatTest.bigDecimal);
  }

  @Override
  public int hashCode() {
    return Objects.hash(integer, int32, int64, number, _float, _double, string, Arrays.hashCode(_byte), binary, date, dateTime, uuid, password, bigDecimal);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class FormatTest {\n");
    
    sb.append("    integer: ").append(toIndentedString(integer)).append("\n");
    sb.append("    int32: ").append(toIndentedString(int32)).append("\n");
    sb.append("    int64: ").append(toIndentedString(int64)).append("\n");
    sb.append("    number: ").append(toIndentedString(number)).append("\n");
    sb.append("    _float: ").append(toIndentedString(_float)).append("\n");
    sb.append("    _double: ").append(toIndentedString(_double)).append("\n");
    sb.append("    string: ").append(toIndentedString(string)).append("\n");
    sb.append("    _byte: ").append(toIndentedString(_byte)).append("\n");
    sb.append("    binary: ").append(toIndentedString(binary)).append("\n");
    sb.append("    date: ").append(toIndentedString(date)).append("\n");
    sb.append("    dateTime: ").append(toIndentedString(dateTime)).append("\n");
    sb.append("    uuid: ").append(toIndentedString(uuid)).append("\n");
    sb.append("    password: ").append("*").append("\n");
    sb.append("    bigDecimal: ").append(toIndentedString(bigDecimal)).append("\n");
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


  public static FormatTestBuilder<?, ?> builder() {
    return new FormatTestBuilderImpl();
  }

  private static final class FormatTestBuilderImpl extends FormatTestBuilder<FormatTest, FormatTestBuilderImpl> {

    @Override
    protected FormatTestBuilderImpl self() {
      return this;
    }

    @Override
    public FormatTest build() {
      return new FormatTest(this);
    }
  }

  public static abstract class FormatTestBuilder<C extends FormatTest, B extends FormatTestBuilder<C, B>>  {
    private Integer integer;
    private Integer int32;
    private Long int64;
    private BigDecimal number;
    private Float _float;
    private Double _double;
    private String string;
    private byte[] _byte;
    private File binary;
    private LocalDate date;
    private Date dateTime;
    private UUID uuid;
    private String password;
    private BigDecimal bigDecimal;
    protected abstract B self();

    public abstract C build();

    public B integer(Integer integer) {
      this.integer = integer;
      return self();
    }
    public B int32(Integer int32) {
      this.int32 = int32;
      return self();
    }
    public B int64(Long int64) {
      this.int64 = int64;
      return self();
    }
    public B number(BigDecimal number) {
      this.number = number;
      return self();
    }
    public B _float(Float _float) {
      this._float = _float;
      return self();
    }
    public B _double(Double _double) {
      this._double = _double;
      return self();
    }
    public B string(String string) {
      this.string = string;
      return self();
    }
    public B _byte(byte[] _byte) {
      this._byte = _byte;
      return self();
    }
    public B binary(File binary) {
      this.binary = binary;
      return self();
    }
    public B date(LocalDate date) {
      this.date = date;
      return self();
    }
    public B dateTime(Date dateTime) {
      this.dateTime = dateTime;
      return self();
    }
    public B uuid(UUID uuid) {
      this.uuid = uuid;
      return self();
    }
    public B password(String password) {
      this.password = password;
      return self();
    }
    public B bigDecimal(BigDecimal bigDecimal) {
      this.bigDecimal = bigDecimal;
      return self();
    }
  }
}


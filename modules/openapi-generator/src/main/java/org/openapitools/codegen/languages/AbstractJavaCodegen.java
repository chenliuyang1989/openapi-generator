/*
 * Copyright 2018 OpenAPI-Generator Contributors (https://openapi-generator.tech)
 * Copyright 2018 SmartBear Software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.openapitools.codegen.languages;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.samskivert.mustache.Mustache;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.examples.Example;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.util.SchemaTypeUtil;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.openapitools.codegen.*;
import org.openapitools.codegen.languages.features.BeanValidationFeatures;
import org.openapitools.codegen.languages.features.DocumentationProviderFeatures;
import org.openapitools.codegen.meta.features.*;
import org.openapitools.codegen.model.ModelMap;
import org.openapitools.codegen.model.ModelsMap;
import org.openapitools.codegen.model.OperationMap;
import org.openapitools.codegen.model.OperationsMap;
import org.openapitools.codegen.templating.mustache.ReplaceAllLambda;
import org.openapitools.codegen.utils.CamelizeOption;
import org.openapitools.codegen.utils.ModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.lang.model.SourceVersion;
import java.io.File;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.openapitools.codegen.utils.CamelizeOption.*;
import static org.openapitools.codegen.utils.ModelUtils.getSchemaItems;
import static org.openapitools.codegen.utils.OnceLogger.once;
import static org.openapitools.codegen.utils.StringUtils.*;

public abstract class AbstractJavaCodegen extends DefaultCodegen implements CodegenConfig,
        DocumentationProviderFeatures {

    private final Logger LOGGER = LoggerFactory.getLogger(AbstractJavaCodegen.class);
    private static final String ARTIFACT_VERSION_DEFAULT_VALUE = "1.0.0";
    private static final ZoneId UTC = ZoneId.of("UTC");

    public static final String DEFAULT_LIBRARY = "<default>";
    public static final String DATE_LIBRARY = "dateLibrary";
    public static final String SUPPORT_ASYNC = "supportAsync";
    public static final String WITH_XML = "withXml";
    public static final String DISABLE_HTML_ESCAPING = "disableHtmlEscaping";
    public static final String BOOLEAN_GETTER_PREFIX = "booleanGetterPrefix";
    public static final String IGNORE_ANYOF_IN_ENUM = "ignoreAnyOfInEnum";
    public static final String ADDITIONAL_MODEL_TYPE_ANNOTATIONS = "additionalModelTypeAnnotations";
    public static final String ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS = "additionalOneOfTypeAnnotations";
    public static final String ADDITIONAL_ENUM_TYPE_ANNOTATIONS = "additionalEnumTypeAnnotations";
    public static final String DISCRIMINATOR_CASE_SENSITIVE = "discriminatorCaseSensitive";
    public static final String OPENAPI_NULLABLE = "openApiNullable";
    public static final String JACKSON = "jackson";
    public static final String TEST_OUTPUT = "testOutput";
    public static final String IMPLICIT_HEADERS = "implicitHeaders";
    public static final String IMPLICIT_HEADERS_REGEX = "implicitHeadersRegex";
    public static final String JAVAX_PACKAGE = "javaxPackage";
    public static final String USE_JAKARTA_EE = "useJakartaEe";
    public static final String CONTAINER_DEFAULT_TO_NULL = "containerDefaultToNull";

    public static final String CAMEL_CASE_DOLLAR_SIGN = "camelCaseDollarSign";
    public static final String USE_ONE_OF_INTERFACES = "useOneOfInterfaces";
    public static final String LOMBOK = "lombok";
    public static final String DEFAULT_TEST_FOLDER = "${project.build.directory}/generated-test-sources/openapi";
    public static final String GENERATE_CONSTRUCTOR_WITH_ALL_ARGS = "generateConstructorWithAllArgs";
    public static final String GENERATE_BUILDERS = "generateBuilders";

    @Getter @Setter
    protected String dateLibrary = "java8";
    @Setter protected boolean supportAsync = false;
    @Setter protected boolean withXml = false;
    @Getter @Setter
    protected String invokerPackage = "org.openapitools";
    @Getter @Setter
    protected String groupId = "org.openapitools";
    @Getter @Setter
    protected String artifactId = "openapi-java";
    @Getter @Setter
    protected String artifactVersion = null;
    @Getter @Setter
    protected String artifactUrl = "https://github.com/openapitools/openapi-generator";
    @Getter @Setter
    protected String artifactDescription = "OpenAPI Java";
    @Getter @Setter
    protected String developerName = "OpenAPI-Generator Contributors";
    @Getter @Setter
    protected String developerEmail = "team@openapitools.org";
    @Getter @Setter
    protected String developerOrganization = "OpenAPITools.org";
    @Getter @Setter
    protected String developerOrganizationUrl = "http://openapitools.org";
    @Getter @Setter
    protected String scmConnection = "scm:git:git@github.com:openapitools/openapi-generator.git";
    @Getter @Setter
    protected String scmDeveloperConnection = "scm:git:git@github.com:openapitools/openapi-generator.git";
    @Getter @Setter
    protected String scmUrl = "https://github.com/openapitools/openapi-generator";
    @Getter @Setter
    protected String licenseName = "Unlicense";
    @Getter @Setter
    protected String licenseUrl = "http://unlicense.org";
    protected String projectFolder = "src/main";
    protected String projectTestFolder = "src/test";
    // this must not be OS-specific
    @Getter @Setter
    protected String sourceFolder = projectFolder + "/java";
    @Getter @Setter
    protected String testFolder = projectTestFolder + "/java";

    protected enum ENUM_PROPERTY_NAMING_TYPE {MACRO_CASE, legacy, original}

    protected static final String ENUM_PROPERTY_NAMING_DESC = "Naming convention for enum properties: 'MACRO_CASE', 'legacy' and 'original'";
    @Getter protected ENUM_PROPERTY_NAMING_TYPE enumPropertyNaming = ENUM_PROPERTY_NAMING_TYPE.MACRO_CASE;

    /**
     * -- SETTER --
     * Set whether discriminator value lookup is case-sensitive or not.
     *
     */
    @Setter protected boolean discriminatorCaseSensitive = true;
    @Getter @Setter
    protected Boolean serializableModel = false;
    @Setter protected boolean serializeBigDecimalAsString = false;
    protected String apiDocPath = "docs/";
    protected String modelDocPath = "docs/";
    @Setter protected boolean disableHtmlEscaping = false;
    @Getter @Setter
    protected String booleanGetterPrefix = "get";
    @Setter protected boolean ignoreAnyOfInEnum = false;
    @Setter protected String parentGroupId = "";
    @Setter protected String parentArtifactId = "";
    @Setter protected String parentVersion = "";
    @Setter protected boolean parentOverridden = false;
    @Getter @Setter
    protected List<String> additionalModelTypeAnnotations = new LinkedList<>();
    protected Map<String, Boolean> lombokAnnotations = null;
    @Getter @Setter
    protected List<String> additionalOneOfTypeAnnotations = new LinkedList<>();
    @Setter protected List<String> additionalEnumTypeAnnotations = new LinkedList<>();
    @Getter @Setter
    protected boolean openApiNullable = true;
    @Setter protected String outputTestFolder = "";
    protected DocumentationProvider documentationProvider;
    protected AnnotationLibrary annotationLibrary;
    @Setter protected boolean implicitHeaders = false;
    @Setter protected String implicitHeadersRegex = null;
    @Setter protected boolean camelCaseDollarSign = false;
    @Setter protected boolean useJakartaEe = false;
    @Setter protected boolean containerDefaultToNull = false;
    @Getter @Setter
    protected boolean generateConstructorWithAllArgs = false;
    @Getter @Setter
    protected boolean jackson = false;
    @Getter @Setter
    protected boolean generateBuilders;
    /**
     * useBeanValidation has been moved from child generators to AbstractJavaCodegen.
     * The reason is that getBeanValidation needs it
     */
    @Getter @Setter
    protected boolean useBeanValidation = false;
    private Map<String, String> schemaKeyToModelNameCache = new HashMap<>();

    public AbstractJavaCodegen() {
        super();

        modifyFeatureSet(features -> features
                .includeDocumentationFeatures(DocumentationFeature.Readme)
                .wireFormatFeatures(EnumSet.of(WireFormatFeature.JSON, WireFormatFeature.XML))
                .securityFeatures(EnumSet.of(
                        SecurityFeature.ApiKey,
                        SecurityFeature.BasicAuth,
                        SecurityFeature.BearerToken,
                        SecurityFeature.OAuth2_Implicit
                ))
                .excludeGlobalFeatures(
                        GlobalFeature.XMLStructureDefinitions,
                        GlobalFeature.Callbacks,
                        GlobalFeature.LinkObjects,
                        GlobalFeature.ParameterStyling
                )
                .excludeSchemaSupportFeatures(
                        SchemaSupportFeature.Polymorphism
                )
                .includeClientModificationFeatures(
                        ClientModificationFeature.BasePath
                )
        );

        supportsInheritance = true;
        modelTemplateFiles.put("model.mustache", ".java");
        apiTemplateFiles.put("api.mustache", ".java");
        apiTestTemplateFiles.put("api_test.mustache", ".java");
        modelDocTemplateFiles.put("model_doc.mustache", ".md");
        apiDocTemplateFiles.put("api_doc.mustache", ".md");

        hideGenerationTimestamp = false;

        setReservedWordsLowerCase(
                Arrays.asList(
                        // special words
                        "object", "list", "file",
                        // used as internal variables, can collide with parameter names
                        "localVarPath", "localVarQueryParams", "localVarCollectionQueryParams",
                        "localVarHeaderParams", "localVarCookieParams", "localVarFormParams", "localVarPostBody",
                        "localVarAccepts", "localVarAccept", "localVarContentTypes",
                        "localVarContentType", "localVarAuthNames", "localReturnType",
                        "ApiClient", "ApiException", "ApiResponse", "Configuration", "StringUtil",

                        // language reserved words
                        "_", "abstract", "continue", "for", "new", "switch", "assert",
                        "default", "if", "package", "synchronized", "boolean", "do", "goto", "private",
                        "this", "break", "double", "implements", "protected", "throw", "byte", "else",
                        "import", "public", "throws", "case", "enum", "instanceof", "return", "transient",
                        "catch", "extends", "int", "short", "try", "char", "final", "interface", "static",
                        "void", "class", "finally", "long", "strictfp", "volatile", "const", "float",
                        "native", "super", "while", "null", "offsetdatetime", "localdate", "localtime")
        );

        languageSpecificPrimitives = Sets.newHashSet("String",
                "boolean",
                "Boolean",
                "Double",
                "Integer",
                "Long",
                "Float",
                "Object",
                "byte[]"
        );
        instantiationTypes.put("array", "ArrayList");
        instantiationTypes.put("set", "LinkedHashSet");
        instantiationTypes.put("map", "HashMap");
        typeMapping.put("date", "Date");
        typeMapping.put("file", "File");
        typeMapping.put("AnyType", "Object");

        importMapping.put("BigDecimal", "java.math.BigDecimal");
        importMapping.put("UUID", "java.util.UUID");
        importMapping.put("URI", "java.net.URI");
        importMapping.put("File", "java.io.File");
        importMapping.put("Date", "java.util.Date");
        importMapping.put("Timestamp", "java.sql.Timestamp");
        importMapping.put("Map", "java.util.Map");
        importMapping.put("HashMap", "java.util.HashMap");
        importMapping.put("Array", "java.util.List");
        importMapping.put("ArrayList", "java.util.ArrayList");
        importMapping.put("List", "java.util.*");
        importMapping.put("Set", "java.util.*");
        importMapping.put("LinkedHashSet", "java.util.LinkedHashSet");
        importMapping.put("DateTime", "org.joda.time.*");
        importMapping.put("LocalDateTime", "org.joda.time.*");
        importMapping.put("LocalDate", "org.joda.time.*");
        importMapping.put("LocalTime", "org.joda.time.*");

        cliOptions.add(new CliOption(CodegenConstants.MODEL_PACKAGE, CodegenConstants.MODEL_PACKAGE_DESC));
        cliOptions.add(new CliOption(CodegenConstants.API_PACKAGE, CodegenConstants.API_PACKAGE_DESC));
        cliOptions.add(new CliOption(CodegenConstants.INVOKER_PACKAGE, CodegenConstants.INVOKER_PACKAGE_DESC).defaultValue(this.getInvokerPackage()));
        cliOptions.add(new CliOption(CodegenConstants.GROUP_ID, CodegenConstants.GROUP_ID_DESC).defaultValue(this.getGroupId()));
        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_ID, CodegenConstants.ARTIFACT_ID_DESC).defaultValue(this.getArtifactId()));
        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_VERSION, CodegenConstants.ARTIFACT_VERSION_DESC).defaultValue(ARTIFACT_VERSION_DEFAULT_VALUE));
        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_URL, CodegenConstants.ARTIFACT_URL_DESC).defaultValue(this.getArtifactUrl()));
        cliOptions.add(new CliOption(CodegenConstants.ARTIFACT_DESCRIPTION, CodegenConstants.ARTIFACT_DESCRIPTION_DESC).defaultValue(this.getArtifactDescription()));
        cliOptions.add(new CliOption(CodegenConstants.SCM_CONNECTION, CodegenConstants.SCM_CONNECTION_DESC).defaultValue(this.getScmConnection()));
        cliOptions.add(new CliOption(CodegenConstants.SCM_DEVELOPER_CONNECTION, CodegenConstants.SCM_DEVELOPER_CONNECTION_DESC).defaultValue(this.getScmDeveloperConnection()));
        cliOptions.add(new CliOption(CodegenConstants.SCM_URL, CodegenConstants.SCM_URL_DESC).defaultValue(this.getScmUrl()));
        cliOptions.add(new CliOption(CodegenConstants.DEVELOPER_NAME, CodegenConstants.DEVELOPER_NAME_DESC).defaultValue(this.getDeveloperName()));
        cliOptions.add(new CliOption(CodegenConstants.DEVELOPER_EMAIL, CodegenConstants.DEVELOPER_EMAIL_DESC).defaultValue(this.getDeveloperEmail()));
        cliOptions.add(new CliOption(CodegenConstants.DEVELOPER_ORGANIZATION, CodegenConstants.DEVELOPER_ORGANIZATION_DESC).defaultValue(this.getDeveloperOrganization()));
        cliOptions.add(new CliOption(CodegenConstants.DEVELOPER_ORGANIZATION_URL, CodegenConstants.DEVELOPER_ORGANIZATION_URL_DESC).defaultValue(this.getDeveloperOrganizationUrl()));
        cliOptions.add(new CliOption(CodegenConstants.LICENSE_NAME, CodegenConstants.LICENSE_NAME_DESC).defaultValue(this.getLicenseName()));
        cliOptions.add(new CliOption(CodegenConstants.LICENSE_URL, CodegenConstants.LICENSE_URL_DESC).defaultValue(this.getLicenseUrl()));
        cliOptions.add(new CliOption(CodegenConstants.SOURCE_FOLDER, CodegenConstants.SOURCE_FOLDER_DESC).defaultValue(this.getSourceFolder()));
        cliOptions.add(CliOption.newBoolean(CodegenConstants.SERIALIZABLE_MODEL, CodegenConstants.SERIALIZABLE_MODEL_DESC, this.getSerializableModel()));
        cliOptions.add(CliOption.newBoolean(CodegenConstants.SERIALIZE_BIG_DECIMAL_AS_STRING, CodegenConstants.SERIALIZE_BIG_DECIMAL_AS_STRING_DESC, serializeBigDecimalAsString));
        cliOptions.add(CliOption.newBoolean(DISCRIMINATOR_CASE_SENSITIVE, "Whether the discriminator value lookup should be case-sensitive or not. This option only works for Java API client", discriminatorCaseSensitive));
        cliOptions.add(CliOption.newBoolean(CodegenConstants.HIDE_GENERATION_TIMESTAMP, CodegenConstants.HIDE_GENERATION_TIMESTAMP_DESC, this.isHideGenerationTimestamp()));
        cliOptions.add(CliOption.newBoolean(WITH_XML, "whether to include support for application/xml content type and include XML annotations in the model (works with libraries that provide support for JSON and XML)"));
        cliOptions.add(CliOption.newBoolean(USE_ONE_OF_INTERFACES, "whether to use a java interface to describe a set of oneOf options, where each option is a class that implements the interface"));

        CliOption dateLibrary = new CliOption(DATE_LIBRARY, "Option. Date library to use").defaultValue(this.getDateLibrary());
        Map<String, String> dateOptions = new HashMap<>();
        dateOptions.put("java8", "Java 8 native JSR310 (preferred for jdk 1.8+)");
        dateOptions.put("java8-localdatetime", "Java 8 using LocalDateTime (for legacy app only)");
        dateOptions.put("joda", "Joda (for legacy app only)");
        dateOptions.put("legacy", "Legacy java.util.Date");
        dateLibrary.setEnum(dateOptions);
        cliOptions.add(dateLibrary);

        cliOptions.add(CliOption.newBoolean(DISABLE_HTML_ESCAPING, "Disable HTML escaping of JSON strings when using gson (needed to avoid problems with byte[] fields)", disableHtmlEscaping));
        cliOptions.add(CliOption.newString(BOOLEAN_GETTER_PREFIX, "Set booleanGetterPrefix").defaultValue(this.getBooleanGetterPrefix()));
        cliOptions.add(CliOption.newBoolean(IGNORE_ANYOF_IN_ENUM, "Ignore anyOf keyword in enum", ignoreAnyOfInEnum));
        cliOptions.add(CliOption.newString(ADDITIONAL_ENUM_TYPE_ANNOTATIONS, "Additional annotations for enum type(class level annotations)"));
        cliOptions.add(CliOption.newString(ADDITIONAL_MODEL_TYPE_ANNOTATIONS, "Additional annotations for model type(class level annotations). List separated by semicolon(;) or new line (Linux or Windows)"));
        cliOptions.add(CliOption.newString(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS, "Additional annotations for oneOf interfaces(class level annotations). List separated by semicolon(;) or new line (Linux or Windows)"));
        cliOptions.add(CliOption.newBoolean(OPENAPI_NULLABLE, "Enable OpenAPI Jackson Nullable library. Not supported by `microprofile` library.", this.openApiNullable));
        cliOptions.add(CliOption.newBoolean(IMPLICIT_HEADERS, "Skip header parameters in the generated API methods using @ApiImplicitParams annotation.", implicitHeaders));
        cliOptions.add(CliOption.newString(IMPLICIT_HEADERS_REGEX, "Skip header parameters that matches given regex in the generated API methods using @ApiImplicitParams annotation. Note: this parameter is ignored when implicitHeaders=true"));
        cliOptions.add(CliOption.newBoolean(CAMEL_CASE_DOLLAR_SIGN, "Fix camelCase when starting with $ sign. when true : $Value when false : $value"));
        cliOptions.add(CliOption.newBoolean(USE_JAKARTA_EE, "whether to use Jakarta EE namespace instead of javax"));
        cliOptions.add(CliOption.newBoolean(CONTAINER_DEFAULT_TO_NULL, "Set containers (array, set, map) default to null"));
        cliOptions.add(CliOption.newBoolean(GENERATE_CONSTRUCTOR_WITH_ALL_ARGS, "whether to generate a constructor for all arguments").defaultValue(Boolean.FALSE.toString()));
        cliOptions.add(CliOption.newBoolean(GENERATE_BUILDERS, "Whether to generate builders for models").defaultValue(Boolean.FALSE.toString()));

        cliOptions.add(CliOption.newString(CodegenConstants.PARENT_GROUP_ID, CodegenConstants.PARENT_GROUP_ID_DESC));
        cliOptions.add(CliOption.newString(CodegenConstants.PARENT_ARTIFACT_ID, CodegenConstants.PARENT_ARTIFACT_ID_DESC));
        cliOptions.add(CliOption.newString(CodegenConstants.PARENT_VERSION, CodegenConstants.PARENT_VERSION_DESC));
        CliOption snapShotVersion = CliOption.newString(CodegenConstants.SNAPSHOT_VERSION, CodegenConstants.SNAPSHOT_VERSION_DESC);
        Map<String, String> snapShotVersionOptions = new HashMap<>();
        snapShotVersionOptions.put("true", "Use a SnapShot Version");
        snapShotVersionOptions.put("false", "Use a Release Version");
        snapShotVersion.setEnum(snapShotVersionOptions);
        cliOptions.add(snapShotVersion);
        cliOptions.add(CliOption.newString(TEST_OUTPUT, "Set output folder for models and APIs tests").defaultValue(DEFAULT_TEST_FOLDER));

        if (null != defaultDocumentationProvider()) {
            CliOption documentationProviderCliOption = new CliOption(DOCUMENTATION_PROVIDER,
                    "Select the OpenAPI documentation provider.")
                    .defaultValue(defaultDocumentationProvider().toCliOptValue());
            supportedDocumentationProvider().forEach(dp ->
                    documentationProviderCliOption.addEnum(dp.toCliOptValue(), dp.getDescription()));
            cliOptions.add(documentationProviderCliOption);

            CliOption annotationLibraryCliOption = new CliOption(ANNOTATION_LIBRARY,
                    "Select the complementary documentation annotation library.")
                    .defaultValue(defaultDocumentationProvider().getPreferredAnnotationLibrary().toCliOptValue());
            supportedAnnotationLibraries().forEach(al ->
                    annotationLibraryCliOption.addEnum(al.toCliOptValue(), al.getDescription()));
            cliOptions.add(annotationLibraryCliOption);
        }

        CliOption enumPropertyNamingOpt = new CliOption(CodegenConstants.ENUM_PROPERTY_NAMING, ENUM_PROPERTY_NAMING_DESC);
        cliOptions.add(enumPropertyNamingOpt.defaultValue(enumPropertyNaming.name()));
    }

    @Override
    public void processOpts() {
        useCodegenAsMustacheParentContext();
        super.processOpts();

        if (null != defaultDocumentationProvider()) {
            documentationProvider = DocumentationProvider.ofCliOption(
                    (String) additionalProperties.getOrDefault(DOCUMENTATION_PROVIDER,
                            defaultDocumentationProvider().toCliOptValue())
            );

            if (!supportedDocumentationProvider().contains(documentationProvider)) {
                String msg = String.format(Locale.ROOT,
                        "The [%s] Documentation Provider is not supported by this generator",
                        documentationProvider.toCliOptValue());
                throw new IllegalArgumentException(msg);
            }

            annotationLibrary = AnnotationLibrary.ofCliOption(
                    (String) additionalProperties.getOrDefault(ANNOTATION_LIBRARY,
                            documentationProvider.getPreferredAnnotationLibrary().toCliOptValue())
            );

            if (!supportedAnnotationLibraries().contains(annotationLibrary)) {
                String msg = String.format(Locale.ROOT, "The Annotation Library [%s] is not supported by this generator",
                        annotationLibrary.toCliOptValue());
                throw new IllegalArgumentException(msg);
            }

            if (!documentationProvider.supportedAnnotationLibraries().contains(annotationLibrary)) {
                String msg = String.format(Locale.ROOT,
                        "The [%s] documentation provider does not support [%s] as complementary annotation library",
                        documentationProvider.toCliOptValue(), annotationLibrary.toCliOptValue());
                throw new IllegalArgumentException(msg);
            }

            additionalProperties.put(DOCUMENTATION_PROVIDER, documentationProvider.toCliOptValue());
            additionalProperties.put(documentationProvider.getPropertyName(), true);
            additionalProperties.put(ANNOTATION_LIBRARY, annotationLibrary.toCliOptValue());
            additionalProperties.put(annotationLibrary.getPropertyName(), true);
        } else {
            additionalProperties.put(DOCUMENTATION_PROVIDER, DocumentationProvider.NONE);
            additionalProperties.put(ANNOTATION_LIBRARY, AnnotationLibrary.NONE);
        }

        convertPropertyToBooleanAndWriteBack(GENERATE_CONSTRUCTOR_WITH_ALL_ARGS, this::setGenerateConstructorWithAllArgs);
        convertPropertyToBooleanAndWriteBack(GENERATE_BUILDERS, this::setGenerateBuilders);
        if (StringUtils.isEmpty(System.getenv("JAVA_POST_PROCESS_FILE"))) {
            LOGGER.info("Environment variable JAVA_POST_PROCESS_FILE not defined so the Java code may not be properly formatted. To define it, try 'export JAVA_POST_PROCESS_FILE=\"/usr/local/bin/clang-format -i\"' (Linux/Mac)");
            LOGGER.info("NOTE: To enable file post-processing, 'enablePostProcessFile' must be set to `true` (--enable-post-process-file for CLI).");
        } else if (!this.isEnablePostProcessFile()) {
            LOGGER.info("Warning: Environment variable 'JAVA_POST_PROCESS_FILE' is set but file post-processing is not enabled. To enable file post-processing, 'enablePostProcessFile' must be set to `true` (--enable-post-process-file for CLI).");
        }

        convertPropertyToBooleanAndWriteBack(BeanValidationFeatures.USE_BEANVALIDATION, this::setUseBeanValidation);
        convertPropertyToBooleanAndWriteBack(DISABLE_HTML_ESCAPING, this::setDisableHtmlEscaping);
        convertPropertyToStringAndWriteBack(BOOLEAN_GETTER_PREFIX, this::setBooleanGetterPrefix);
        convertPropertyToBooleanAndWriteBack(IGNORE_ANYOF_IN_ENUM, this::setIgnoreAnyOfInEnum);
        convertPropertyToTypeAndWriteBack(ADDITIONAL_MODEL_TYPE_ANNOTATIONS,
                annotations -> Arrays.asList(annotations.trim().split("\\s*(;|\\r?\\n)\\s*")),
                this::setAdditionalModelTypeAnnotations);
        convertPropertyToTypeAndWriteBack(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS,
                annotations -> Arrays.asList(annotations.trim().split("\\s*(;|\\r?\\n)\\s*")),
                this::setAdditionalOneOfTypeAnnotations);
        convertPropertyToTypeAndWriteBack(ADDITIONAL_ENUM_TYPE_ANNOTATIONS,
                annotations -> Arrays.asList(annotations.split(";")),
                this::setAdditionalEnumTypeAnnotations);

        if (additionalProperties.containsKey(CodegenConstants.INVOKER_PACKAGE)) {
            this.setInvokerPackage((String) additionalProperties.get(CodegenConstants.INVOKER_PACKAGE));
        } else if (additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            // guess from api package
            String derivedInvokerPackage = deriveInvokerPackageName((String) additionalProperties.get(CodegenConstants.API_PACKAGE));
            this.setInvokerPackage(derivedInvokerPackage);
            LOGGER.info("Invoker Package Name, originally not set, is now derived from api package name: {}", derivedInvokerPackage);
        } else if (additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            // guess from model package
            String derivedInvokerPackage = deriveInvokerPackageName((String) additionalProperties.get(CodegenConstants.MODEL_PACKAGE));
            this.setInvokerPackage(derivedInvokerPackage);
            LOGGER.info("Invoker Package Name, originally not set, is now derived from model package name: {}",
                    derivedInvokerPackage);
        } else {
            //not set, use default to be passed to template
            additionalProperties.put(CodegenConstants.INVOKER_PACKAGE, invokerPackage);
        }

        if (!additionalProperties.containsKey(CodegenConstants.MODEL_PACKAGE)) {
            additionalProperties.put(CodegenConstants.MODEL_PACKAGE, modelPackage);
        }

        if (!additionalProperties.containsKey(CodegenConstants.API_PACKAGE)) {
            additionalProperties.put(CodegenConstants.API_PACKAGE, apiPackage);
        }

        if (additionalProperties.containsKey(CodegenConstants.GROUP_ID)) {
            this.setGroupId((String) additionalProperties.get(CodegenConstants.GROUP_ID));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(CodegenConstants.GROUP_ID, groupId);
        }

        if (additionalProperties.containsKey(CodegenConstants.ARTIFACT_ID)) {
            this.setArtifactId((String) additionalProperties.get(CodegenConstants.ARTIFACT_ID));
        } else {
            //not set, use to be passed to template
            additionalProperties.put(CodegenConstants.ARTIFACT_ID, artifactId);
        }

        if (additionalProperties.containsKey(CodegenConstants.ARTIFACT_URL)) {
            this.setArtifactUrl((String) additionalProperties.get(CodegenConstants.ARTIFACT_URL));
        } else {
            additionalProperties.put(CodegenConstants.ARTIFACT_URL, artifactUrl);
        }

        if (additionalProperties.containsKey(CodegenConstants.ARTIFACT_DESCRIPTION)) {
            this.setArtifactDescription((String) additionalProperties.get(CodegenConstants.ARTIFACT_DESCRIPTION));
        } else {
            additionalProperties.put(CodegenConstants.ARTIFACT_DESCRIPTION, artifactDescription);
        }

        if (additionalProperties.containsKey(CodegenConstants.SCM_CONNECTION)) {
            this.setScmConnection((String) additionalProperties.get(CodegenConstants.SCM_CONNECTION));
        } else {
            additionalProperties.put(CodegenConstants.SCM_CONNECTION, scmConnection);
        }

        if (additionalProperties.containsKey(CodegenConstants.SCM_DEVELOPER_CONNECTION)) {
            this.setScmDeveloperConnection((String) additionalProperties.get(CodegenConstants.SCM_DEVELOPER_CONNECTION));
        } else {
            additionalProperties.put(CodegenConstants.SCM_DEVELOPER_CONNECTION, scmDeveloperConnection);
        }

        if (additionalProperties.containsKey(CodegenConstants.SCM_URL)) {
            this.setScmUrl((String) additionalProperties.get(CodegenConstants.SCM_URL));
        } else {
            additionalProperties.put(CodegenConstants.SCM_URL, scmUrl);
        }

        if (additionalProperties.containsKey(CodegenConstants.DEVELOPER_NAME)) {
            this.setDeveloperName((String) additionalProperties.get(CodegenConstants.DEVELOPER_NAME));
        } else {
            additionalProperties.put(CodegenConstants.DEVELOPER_NAME, developerName);
        }

        if (additionalProperties.containsKey(CodegenConstants.DEVELOPER_EMAIL)) {
            this.setDeveloperEmail((String) additionalProperties.get(CodegenConstants.DEVELOPER_EMAIL));
        } else {
            additionalProperties.put(CodegenConstants.DEVELOPER_EMAIL, developerEmail);
        }

        if (additionalProperties.containsKey(CodegenConstants.DEVELOPER_ORGANIZATION)) {
            this.setDeveloperOrganization((String) additionalProperties.get(CodegenConstants.DEVELOPER_ORGANIZATION));
        } else {
            additionalProperties.put(CodegenConstants.DEVELOPER_ORGANIZATION, developerOrganization);
        }

        if (additionalProperties.containsKey(CodegenConstants.DEVELOPER_ORGANIZATION_URL)) {
            this.setDeveloperOrganizationUrl((String) additionalProperties.get(CodegenConstants.DEVELOPER_ORGANIZATION_URL));
        } else {
            additionalProperties.put(CodegenConstants.DEVELOPER_ORGANIZATION_URL, developerOrganizationUrl);
        }

        convertPropertyToStringAndWriteBack(CodegenConstants.MODEL_PACKAGE, this::setModelPackage);
        convertPropertyToStringAndWriteBack(CodegenConstants.API_PACKAGE, this::setApiPackage);
        convertPropertyToStringAndWriteBack(CodegenConstants.GROUP_ID, this::setGroupId);
        convertPropertyToStringAndWriteBack(CodegenConstants.ARTIFACT_ID, this::setArtifactId);
        convertPropertyToStringAndWriteBack(CodegenConstants.ARTIFACT_URL, this::setArtifactUrl);
        convertPropertyToStringAndWriteBack(CodegenConstants.ARTIFACT_DESCRIPTION, this::setArtifactDescription);
        convertPropertyToStringAndWriteBack(CodegenConstants.SCM_CONNECTION, this::setScmConnection);
        convertPropertyToStringAndWriteBack(CodegenConstants.SCM_DEVELOPER_CONNECTION, this::setScmDeveloperConnection);
        convertPropertyToStringAndWriteBack(CodegenConstants.SCM_URL, this::setScmUrl);
        convertPropertyToStringAndWriteBack(CodegenConstants.DEVELOPER_NAME, this::setDeveloperName);
        convertPropertyToStringAndWriteBack(CodegenConstants.DEVELOPER_EMAIL, this::setDeveloperEmail);
        convertPropertyToStringAndWriteBack(CodegenConstants.DEVELOPER_ORGANIZATION, this::setDeveloperOrganization);
        convertPropertyToStringAndWriteBack(CodegenConstants.DEVELOPER_ORGANIZATION_URL, this::setDeveloperOrganizationUrl);
        convertPropertyToStringAndWriteBack(CodegenConstants.LICENSE_NAME, this::setLicenseName);
        convertPropertyToStringAndWriteBack(CodegenConstants.LICENSE_URL, this::setLicenseUrl);
        convertPropertyToStringAndWriteBack(CodegenConstants.SOURCE_FOLDER, this::setSourceFolder);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.SERIALIZABLE_MODEL, this::setSerializableModel);
        convertPropertyToStringAndWriteBack(CodegenConstants.LIBRARY, this::setLibrary);
        convertPropertyToBooleanAndWriteBack(CodegenConstants.SERIALIZE_BIG_DECIMAL_AS_STRING, this::setSerializeBigDecimalAsString);
        // need to put back serializableModel (boolean) into additionalProperties as value in additionalProperties is string
        // additionalProperties.put(CodegenConstants.SERIALIZABLE_MODEL, serializableModel);

        // By default, the discriminator lookup should be case sensitive. There is nothing in the OpenAPI specification
        // that indicates the lookup should be case insensitive. However, some implementations perform
        // a case-insensitive lookup.
        convertPropertyToBooleanAndWriteBack(DISCRIMINATOR_CASE_SENSITIVE, this::setDiscriminatorCaseSensitive);
        convertPropertyToBooleanAndWriteBack(WITH_XML, this::setWithXml);
        convertPropertyToBooleanAndWriteBack(OPENAPI_NULLABLE, this::setOpenApiNullable);
        convertPropertyToStringAndWriteBack(CodegenConstants.PARENT_GROUP_ID, this::setParentGroupId);
        convertPropertyToStringAndWriteBack(CodegenConstants.PARENT_ARTIFACT_ID, this::setParentArtifactId);
        convertPropertyToStringAndWriteBack(CodegenConstants.PARENT_VERSION, this::setParentVersion);
        convertPropertyToBooleanAndWriteBack(IMPLICIT_HEADERS, this::setImplicitHeaders);
        convertPropertyToStringAndWriteBack(IMPLICIT_HEADERS_REGEX, this::setImplicitHeadersRegex);
        convertPropertyToBooleanAndWriteBack(CAMEL_CASE_DOLLAR_SIGN, this::setCamelCaseDollarSign);
        convertPropertyToBooleanAndWriteBack(USE_ONE_OF_INTERFACES, this::setUseOneOfInterfaces);
        convertPropertyToStringAndWriteBack(CodegenConstants.ENUM_PROPERTY_NAMING, this::setEnumPropertyNaming);

        if (!StringUtils.isEmpty(parentGroupId) && !StringUtils.isEmpty(parentArtifactId) && !StringUtils.isEmpty(parentVersion)) {
            additionalProperties.put("parentOverridden", true);
        }

        // make api and model doc path available in mustache template
        additionalProperties.put("apiDocPath", apiDocPath);
        additionalProperties.put("modelDocPath", modelDocPath);

        importMapping.put("List", "java.util.List");
        importMapping.put("Set", "java.util.Set");

        this.sanitizeConfig();

        // optional jackson mappings for BigDecimal support
        if (serializeBigDecimalAsString) {
            importMapping.put("JsonFormat", "com.fasterxml.jackson.annotation.JsonFormat");
        }

        // imports for pojos
        importMapping.put("ApiModelProperty", "io.swagger.annotations.ApiModelProperty");
        importMapping.put("ApiModel", "io.swagger.annotations.ApiModel");
        importMapping.put("Schema", "io.swagger.v3.oas.annotations.media.Schema");
        importMapping.put("BigDecimal", "java.math.BigDecimal");
        importMapping.put("JsonDeserialize", "com.fasterxml.jackson.databind.annotation.JsonDeserialize");
        importMapping.put("JsonProperty", "com.fasterxml.jackson.annotation.JsonProperty");
        importMapping.put("JsonSubTypes", "com.fasterxml.jackson.annotation.JsonSubTypes");
        importMapping.put("JsonTypeInfo", "com.fasterxml.jackson.annotation.JsonTypeInfo");
        importMapping.put("JsonTypeName", "com.fasterxml.jackson.annotation.JsonTypeName");
        importMapping.put("JsonCreator", "com.fasterxml.jackson.annotation.JsonCreator");
        importMapping.put("JsonValue", "com.fasterxml.jackson.annotation.JsonValue");
        importMapping.put("JsonIgnore", "com.fasterxml.jackson.annotation.JsonIgnore");
        importMapping.put("JsonIgnoreProperties", "com.fasterxml.jackson.annotation.JsonIgnoreProperties");
        importMapping.put("JsonInclude", "com.fasterxml.jackson.annotation.JsonInclude");
        if (openApiNullable) {
            importMapping.put("JsonNullable", "org.openapitools.jackson.nullable.JsonNullable");
        }
        importMapping.put("SerializedName", "com.google.gson.annotations.SerializedName");
        importMapping.put("TypeAdapter", "com.google.gson.TypeAdapter");
        importMapping.put("JsonAdapter", "com.google.gson.annotations.JsonAdapter");
        importMapping.put("JsonReader", "com.google.gson.stream.JsonReader");
        importMapping.put("JsonWriter", "com.google.gson.stream.JsonWriter");
        importMapping.put("IOException", "java.io.IOException");
        importMapping.put("Arrays", "java.util.Arrays");
        importMapping.put("Objects", "java.util.Objects");
        importMapping.put("StringUtil", invokerPackage + ".StringUtil");
        // import JsonCreator if JsonProperty is imported
        // used later in recursive import in postProcessingModels
        importMapping.put("com.fasterxml.jackson.annotation.JsonProperty", "com.fasterxml.jackson.annotation.JsonCreator");

        convertPropertyToBooleanAndWriteBack(SUPPORT_ASYNC, this::setSupportAsync);
        convertPropertyToStringAndWriteBack(DATE_LIBRARY, this::setDateLibrary);

        if ("joda".equals(dateLibrary)) {
            additionalProperties.put("joda", "true");
            typeMapping.put("date", "LocalDate");
            typeMapping.put("DateTime", "DateTime");
            importMapping.put("LocalDate", "org.joda.time.LocalDate");
            importMapping.put("DateTime", "org.joda.time.DateTime");
        } else if (dateLibrary.startsWith("java8")) {
            additionalProperties.put("java8", "true");
            additionalProperties.put("jsr310", "true");
            typeMapping.put("date", "LocalDate");
            importMapping.put("LocalDate", "java.time.LocalDate");
            importMapping.put("LocalTime", "java.time.LocalTime");
            if ("java8-localdatetime".equals(dateLibrary)) {
                typeMapping.put("DateTime", "LocalDateTime");
                importMapping.put("LocalDateTime", "java.time.LocalDateTime");
            } else {
                typeMapping.put("DateTime", "OffsetDateTime");
                importMapping.put("OffsetDateTime", "java.time.OffsetDateTime");
            }
        } else if (dateLibrary.equals("legacy")) {
            additionalProperties.put("legacyDates", "true");
        }

        convertPropertyToStringAndWriteBack(TEST_OUTPUT, this::setOutputTestFolder);
        convertPropertyToBooleanAndWriteBack(USE_JAKARTA_EE, this::setUseJakartaEe);
        if (useJakartaEe) {
            applyJakartaPackage();
        } else {
            applyJavaxPackage();
        }

        convertPropertyToBooleanAndWriteBack(CONTAINER_DEFAULT_TO_NULL, this::setContainerDefaultToNull);

        additionalProperties.put("sanitizeGeneric", (Mustache.Lambda) (fragment, writer) -> {
            String content = removeAnnotations(fragment.execute());
            for (final String s : List.of("<", ">", ",", " ")) {
                content = content.replace(s, "");
            }
            writer.write(content);
        });
        additionalProperties.put("removeAnnotations", (Mustache.Lambda) (fragment, writer) -> {
            writer.write(removeAnnotations(fragment.execute()));
        });
        additionalProperties.put("sanitizeDataType", (Mustache.Lambda) (fragment, writer) -> {
            writer.write(sanitizeDataType(fragment.execute()));
        });
    }

    /**
     * Analyse and post process all Models.
     *
     * @param objs the models map.
     * @return the processed models map.
     **/
    @Override
    public Map<String, ModelsMap> postProcessAllModels(Map<String, ModelsMap> objs) {
        objs = super.postProcessAllModels(objs);
        objs = super.updateAllModels(objs);

        Map<String, CodegenModel> allModels = getAllModels(objs);

        if (!additionalModelTypeAnnotations.isEmpty()) {
            for (String modelName : objs.keySet()) {
                Map<String, Object> models = objs.get(modelName);
                models.put(ADDITIONAL_MODEL_TYPE_ANNOTATIONS, additionalModelTypeAnnotations);
            }
        }

        if (!additionalOneOfTypeAnnotations.isEmpty()) {
            for (String modelName : objs.keySet()) {
                Map<String, Object> models = objs.get(modelName);
                models.put(ADDITIONAL_ONE_OF_TYPE_ANNOTATIONS, additionalOneOfTypeAnnotations);
            }
        }

        if (!additionalEnumTypeAnnotations.isEmpty()) {
            for (String modelName : objs.keySet()) {
                Map<String, Object> models = objs.get(modelName);
                models.put(ADDITIONAL_ENUM_TYPE_ANNOTATIONS, additionalEnumTypeAnnotations);
            }
        }

        /*
         Add parentVars and parentRequiredVars to every Model which has a parent.
         Add isInherited to every model which has children.
         This allows
            the generation of fluent setter methods for inherited properties
            the generation of all arg constructors
         ps: This code was specific to SpringCodeGen and now is available to all java generators.
        */
        for (ModelsMap modelsAttrs : objs.values()) {
            for (ModelMap mo : modelsAttrs.getModels()) {
                CodegenModel codegenModel = mo.getModel();
                Set<String> inheritedImports = new HashSet<>();
                Map<String, CodegenProperty> propertyHash = new HashMap<>(codegenModel.vars.size());
                for (final CodegenProperty property : codegenModel.vars) {
                    propertyHash.put(property.name, property);
                }
                List<CodegenModel> parentModelList = getParentModelList(codegenModel);
                for (CodegenModel parentCodegenModel : parentModelList) {
                    for (final CodegenProperty property : parentCodegenModel.vars) {
                        // helper list of parentVars simplifies templating
                        if (!propertyHash.containsKey(property.name)) {
                            propertyHash.put(property.name, property);
                            final CodegenProperty parentVar = property.clone();
                            parentVar.isInherited = true;
                            LOGGER.debug("adding parent variable {} to {}", property.name, codegenModel.name);
                            codegenModel.parentVars.add(parentVar);
                            Set<String> imports = parentVar.getImports(true, this.importBaseType, generatorMetadata.getFeatureSet()).stream().filter(Objects::nonNull).collect(Collectors.toSet());
                            for (String imp : imports) {
                                // Avoid dupes
                                if (!codegenModel.getImports().contains(imp)) {
                                    inheritedImports.add(imp);
                                    codegenModel.getImports().add(imp);
                                }
                            }
                        }
                    }
                }
                if (codegenModel.getParentModel() != null) {
                    codegenModel.parentRequiredVars = new ArrayList<>(codegenModel.getParentModel().requiredVars);
                }
                // There must be a better way ...
                for (String imp : inheritedImports) {
                    String qimp = importMapping().get(imp);
                    if (qimp != null) {
                        Map<String, String> toAdd = new HashMap<>();
                        toAdd.put("import", qimp);
                        modelsAttrs.getImports().add(toAdd);
                    }
                }
            }
        }

        if (isGenerateConstructorWithAllArgs()) {
            // conditionally force the generation of all args constructor.
            for (CodegenModel cm : allModels.values()) {
                if (isConstructorWithAllArgsAllowed(cm)) {
                    cm.vendorExtensions.put("x-java-all-args-constructor", true);
                    List<Object> constructorArgs = new ArrayList<>();
                    // vendorExtensions.x-java-all-args-constructor-vars should be equivalent to allVars
                    // but it is not reliable when openapiNormalizer.REFACTOR_ALLOF_WITH_PROPERTIES_ONLY is disabled
                    cm.vendorExtensions.put("x-java-all-args-constructor-vars", constructorArgs);
                    if (cm.vars.size() + cm.parentVars.size() != cm.allVars.size()) {
                        once(LOGGER).warn("Unexpected allVars for {} expecting:{} vars. actual:{} vars", cm.name, cm.vars.size() + cm.parentVars.size(), cm.allVars.size());
                    }
                    constructorArgs.addAll(cm.vars);
                    constructorArgs.addAll(cm.parentVars);
                }
            }
        }

        return objs;
    }

    private List<CodegenModel> getParentModelList(CodegenModel codegenModel) {
        CodegenModel parentCodegenModel = codegenModel.parentModel;
        List<CodegenModel> parentModelList = new ArrayList<>();
        while (parentCodegenModel != null) {
            parentModelList.add(parentCodegenModel);
            parentCodegenModel = parentCodegenModel.parentModel;
        }
        return parentModelList;
    }

    /**
     * trigger the generation of all arguments constructor or not.
     * It avoids generating the same constructor twice.
     *
     * @return true if an allArgConstructor must be generated
     */
    protected boolean isConstructorWithAllArgsAllowed(CodegenModel codegenModel) {
        // implementation detail: allVars is not reliable if openapiNormalizer.REFACTOR_ALLOF_WITH_PROPERTIES_ONLY is disabled
        return (this.generateConstructorWithAllArgs &&
                (!codegenModel.vars.isEmpty() || codegenModel.parentVars.isEmpty()));
    }

    private void sanitizeConfig() {
        // Sanitize any config options here. We also have to update the additionalProperties because
        // the whole additionalProperties object is injected into the main object passed to the mustache layer

        this.setApiPackage(sanitizePackageName(apiPackage));
        additionalProperties.remove(CodegenConstants.API_PACKAGE);
        this.setModelPackage(sanitizePackageName(modelPackage));
        additionalProperties.remove(CodegenConstants.MODEL_PACKAGE);
        this.setInvokerPackage(sanitizePackageName(invokerPackage));
        additionalProperties.remove(CodegenConstants.INVOKER_PACKAGE);
    }

    protected void applyJavaxPackage() {
        writePropertyBack(JAVAX_PACKAGE, "javax");
    }

    protected void applyJakartaPackage() {
        writePropertyBack(JAVAX_PACKAGE, "jakarta");
    }

    @Override
    public String escapeReservedWord(String name) {
        if (this.reservedWordsMappings().containsKey(name)) {
            return this.reservedWordsMappings().get(name);
        }
        return "_" + name;
    }

    @Override
    public String apiFileFolder() {
        return (outputFolder + File.separator + sourceFolder + File.separator + apiPackage().replace('.', File.separatorChar)).replace('/', File.separatorChar);
    }

    @Override
    public String apiTestFileFolder() {
        return (outputTestFolder + File.separator + testFolder + File.separator + apiPackage().replace('.', File.separatorChar)).replace('/', File.separatorChar);
    }

    @Override
    public String modelTestFileFolder() {
        return (outputTestFolder + File.separator + testFolder + File.separator + modelPackage().replace('.', File.separatorChar)).replace('/', File.separatorChar);
    }

    @Override
    public String modelFileFolder() {
        return (outputFolder + File.separator + sourceFolder + File.separator + modelPackage().replace('.', File.separatorChar)).replace('/', File.separatorChar);
    }

    @Override
    public String apiDocFileFolder() {
        return (outputFolder + File.separator + apiDocPath).replace('/', File.separatorChar);
    }

    @Override
    public String modelDocFileFolder() {
        return (outputFolder + File.separator + modelDocPath).replace('/', File.separatorChar);
    }

    @Override
    public String toApiDocFilename(String name) {
        return toApiName(name);
    }

    @Override
    public String toModelDocFilename(String name) {
        return toModelName(name);
    }

    @Override
    public String toApiTestFilename(String name) {
        return toApiName(name) + "Test";
    }

    @Override
    public String toModelTestFilename(String name) {
        return toModelName(name) + "Test";
    }

    @Override
    public String toApiFilename(String name) {
        return toApiName(name);
    }

    @Override
    public String toVarName(String name) {
        // obtain the name from nameMapping directly if provided
        if (nameMapping.containsKey(name)) {
            return nameMapping.get(name);
        }

        // sanitize name
        name = sanitizeName(name, "\\W-[\\$]"); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.

        if (name.toLowerCase(Locale.ROOT).matches("^_*class$")) {
            return "propertyClass";
        }

        if ("_".equals(name)) {
            name = "_u";
        }

        // numbers are not allowed at the beginning
        if (name.matches("^\\d.*")) {
            name = "_" + name;
        }

        // if it's all upper case, do nothing
        if (name.matches("^[A-Z0-9_]*$")) {
            return name;
        }

        if (startsWithTwoUppercaseLetters(name)) {
            name = name.substring(0, 2).toLowerCase(Locale.ROOT) + name.substring(2);
        }

        // If name contains special chars -> replace them.
        if ((((CharSequence) name).chars().anyMatch(character -> specialCharReplacements.containsKey(String.valueOf((char) character))))) {
            List<String> allowedCharacters = new ArrayList<>();
            allowedCharacters.add("_");
            allowedCharacters.add("$");
            name = escape(name, specialCharReplacements, allowedCharacters, "_");
        }

        // camelize (lower first character) the variable name
        // pet_id => petId
        if (camelCaseDollarSign) {
            name = camelize(name, LOWERCASE_FIRST_CHAR);
        } else {
            name = camelize(name, LOWERCASE_FIRST_LETTER);
        }

        // for reserved word or word starting with number, append _
        if (isReservedWord(name) || name.matches("^\\d.*")) {
            name = escapeReservedWord(name);
        }

        return name;
    }

    private boolean startsWithTwoUppercaseLetters(String name) {
        boolean startsWithTwoUppercaseLetters = false;
        if (name.length() > 1) {
            startsWithTwoUppercaseLetters = name.substring(0, 2).equals(name.substring(0, 2).toUpperCase(Locale.ROOT));
        }
        return startsWithTwoUppercaseLetters;
    }

    @Override
    public String toParamName(String name) {
        // obtain the name from parameterNameMapping  directly if provided
        if (parameterNameMapping.containsKey(name)) {
            return parameterNameMapping.get(name);
        }

        // to avoid conflicts with 'callback' parameter for async call
        if ("callback".equals(name)) {
            return "paramCallback";
        }

        // should be the same as variable name
        return toVarName(name);
    }

    @Override
    public String toModelName(final String name) {
        // obtain the name from modelNameMapping directly if provided
        if (modelNameMapping.containsKey(name)) {
            return modelNameMapping.get(name);
        }

        // We need to check if schema-mapping has a different model for this class, so we use it
        // instead of the auto-generated one.
        if (schemaMapping.containsKey(name)) {
            return schemaMapping.get(name);
        }

        // memoization
        String origName = name;
        if (schemaKeyToModelNameCache.containsKey(origName)) {
            return schemaKeyToModelNameCache.get(origName);
        }

        final String sanitizedName = sanitizeName(name);

        String nameWithPrefixSuffix = sanitizedName;
        if (!StringUtils.isEmpty(modelNamePrefix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = modelNamePrefix + "_" + nameWithPrefixSuffix;
        }

        if (!StringUtils.isEmpty(modelNameSuffix)) {
            // add '_' so that model name can be camelized correctly
            nameWithPrefixSuffix = nameWithPrefixSuffix + "_" + modelNameSuffix;
        }

        // camelize the model name
        // phone_number => PhoneNumber
        final String camelizedName = camelize(nameWithPrefixSuffix);

        // model name cannot use reserved keyword, e.g. return
        if (isReservedWord(camelizedName)) {
            final String modelName = "Model" + camelizedName;
            schemaKeyToModelNameCache.put(origName, modelName);
            LOGGER.warn("{} (reserved word) cannot be used as model name. Renamed to {}", camelizedName, modelName);
            return modelName;
        }

        // model name starts with number
        if (camelizedName.matches("^\\d.*")) {
            final String modelName = "Model" + camelizedName; // e.g. 200Response => Model200Response (after camelize)
            schemaKeyToModelNameCache.put(origName, modelName);
            LOGGER.warn("{} (model name starts with number) cannot be used as model name. Renamed to {}", name,
                    modelName);
            return modelName;
        }

        schemaKeyToModelNameCache.put(origName, camelizedName);

        return camelizedName;
    }

    @Override
    public String toModelFilename(String name) {
        // should be the same as the model name
        return toModelName(name);
    }

    @Override
    public String getTypeDeclaration(Schema p) {
        Schema<?> schema = unaliasSchema(p);
        Schema<?> target = ModelUtils.isGenerateAliasAsModel() ? p : schema;
        if (ModelUtils.isArraySchema(target)) {
            Schema<?> items = getSchemaItems(schema);
            String typeDeclaration = getTypeDeclarationForArray(items);
            return getSchemaType(target) + "<" + typeDeclaration + ">";
        } else if (ModelUtils.isMapSchema(target)) {
            // Note: ModelUtils.isMapSchema(p) returns true when p is a composed schema that also defines
            // additionalproperties: true
            Schema<?> inner = ModelUtils.getAdditionalProperties(target);
            if (inner == null) {
                LOGGER.error("`{}` (map property) does not have a proper inner type defined. Default to type:string", p.getName());
                inner = new StringSchema().description("TODO default missing map inner type to string");
                p.setAdditionalProperties(inner);
            }
            return getSchemaType(target) + "<String, " + getTypeDeclaration(inner) + ">";
        }
        return super.getTypeDeclaration(target);
    }

    private String getTypeDeclarationForArray(Schema<?> items) {
        String typeDeclaration = getTypeDeclaration(items);

        String beanValidation = getBeanValidation(items);
        if (StringUtils.isEmpty(beanValidation)) {
            return typeDeclaration;
        }
        int idxLt = typeDeclaration.indexOf('<');

        int idx = idxLt < 0 ?
                typeDeclaration.lastIndexOf('.') :
                // last dot before the generic like in List<com.mycompany.Container<java.lang.Object>
                typeDeclaration.substring(0, idxLt).lastIndexOf('.');
        if (idx > 0) {
            // fix full qualified name, we need List<java.lang.@Valid String>
            // or List<com.mycompany.@Valid Container<java.lang.Object>
            return typeDeclaration.substring(0, idx + 1) + beanValidation
                    + typeDeclaration.substring(idx + 1);
        } else {
            return beanValidation + typeDeclaration;
        }
    }

    /**
     * This method stand for resolve bean validation for container(array, set).
     * Return empty if there's no bean validation for requested type or prop useBeanValidation false or missed.
     *
     * @param items type
     * @return BeanValidation for declared type in container(array, set)
     */
    private String getBeanValidation(Schema<?> items) {
        if (!isUseBeanValidation()) {
            return "";
        }

        if (ModelUtils.isTypeObjectSchema(items)) {
            // prevents generation '@Valid' for Object
            return "";
        }

        if (items.get$ref() != null) {
            Map<String, Schema> schemas = this.openAPI.getComponents().getSchemas();
            String ref = ModelUtils.getSimpleRef(items.get$ref());
            if (ref != null) {
                Schema<?> schema = schemas.get(ref);
                if (schema == null || ModelUtils.isObjectSchema(schema)) {
                    return "@Valid ";
                }
                items = schema;
            }
        }

        if (ModelUtils.isStringSchema(items)) {
            return getStringBeanValidation(items);
        }

        if (ModelUtils.isNumberSchema(items)) {
            return getNumberBeanValidation(items);
        }

        if (ModelUtils.isLongSchema(items)) {
            return getLongBeanValidation(items);
        }

        if (ModelUtils.isIntegerSchema(items)) {
            return getIntegerBeanValidation(items);
        }

        return "";
    }

    private String getIntegerBeanValidation(Schema<?> items) {
        if (items.getMinimum() != null && items.getMaximum() != null) {
            return String.format(Locale.ROOT, "@Min(%s) @Max(%s)", items.getMinimum(), items.getMaximum());
        }

        if (items.getMinimum() != null) {
            return String.format(Locale.ROOT, "@Min(%s)", items.getMinimum());
        }

        if (items.getMaximum() != null) {
            return String.format(Locale.ROOT, "@Max(%s)", items.getMaximum());
        }
        return "";
    }

    private String getLongBeanValidation(Schema<?> items) {
        if (items.getMinimum() != null && items.getMaximum() != null) {
            return String.format(Locale.ROOT, "@Min(%sL) @Max(%sL)", items.getMinimum(), items.getMaximum());
        }

        if (items.getMinimum() != null) {
            return String.format(Locale.ROOT, "@Min(%sL)", items.getMinimum());
        }

        if (items.getMaximum() != null) {
            return String.format(Locale.ROOT, "@Max(%sL)", items.getMaximum());
        }
        return "";
    }

    private String getNumberBeanValidation(Schema<?> items) {
        if (items.getMinimum() != null && items.getMaximum() != null) {
            return String.format(Locale.ROOT, "@DecimalMin(value = \"%s\", inclusive = %s) @DecimalMax(value = \"%s\", inclusive = %s)",
                    items.getMinimum(),
                    !Optional.ofNullable(items.getExclusiveMinimum()).orElse(Boolean.FALSE),
                    items.getMaximum(),
                    !Optional.ofNullable(items.getExclusiveMaximum()).orElse(Boolean.FALSE));
        }

        if (items.getMinimum() != null) {
            return String.format(Locale.ROOT, "@DecimalMin( value = \"%s\", inclusive = %s)",
                    items.getMinimum(),
                    !Optional.ofNullable(items.getExclusiveMinimum()).orElse(Boolean.FALSE));
        }

        if (items.getMaximum() != null) {
            return String.format(Locale.ROOT, "@DecimalMax( value = \"%s\", inclusive = %s)",
                    items.getMaximum(),
                    !Optional.ofNullable(items.getExclusiveMaximum()).orElse(Boolean.FALSE));
        }

        return "";
    }

    private String getStringBeanValidation(Schema<?> items) {
        String validations = "";
        if (ModelUtils.shouldIgnoreBeanValidation(items)) {
            return validations;
        }

        if (StringUtils.isNotEmpty(items.getPattern())) {
            final String pattern = escapeUnsafeCharacters(
                    StringEscapeUtils.unescapeJava(
                                    StringEscapeUtils.escapeJava(items.getPattern())
                                            .replace("\\/", "/"))
                            .replaceAll("[\\t\\n\\r]", " ")
                            .replace("\\", "\\\\")
                            .replace("\"", "\\\""));

            validations = String.format(Locale.ROOT, "@Pattern(regexp = \"%s\")", pattern);
        }

        if (ModelUtils.isEmailSchema(items)) {
            return String.join("", "@Email ");
        }

        if (ModelUtils.isDecimalSchema(items)) {
            return String.join("", validations, getNumberBeanValidation(items));
        }

        if (items.getMinLength() != null && items.getMaxLength() != null) {
            return String.join("",
                    validations,
                    String.format(Locale.ROOT, "@Size(min = %d, max = %d)", items.getMinLength(), items.getMaxLength()));
        }

        if (items.getMinLength() != null) {
            return String.join("",
                    validations,
                    String.format(Locale.ROOT, "@Size(min = %d)", items.getMinLength()));
        }

        if (items.getMaxLength() != null) {
            return String.join("",
                    validations,
                    String.format(Locale.ROOT, "@Size(max = %d)", items.getMaxLength()));
        }

        return validations;
    }

    /**
     * Return the default value of array property
     * <p>
     * Return null if there's no default value.
     * Any non-null value will cause {{#defaultValue} check to pass.
     *
     * @param cp     Codegen property
     * @param schema Property schema
     * @return string presentation of the default value of the property
     */
    public String toArrayDefaultValue(CodegenProperty cp, Schema schema) {
        if (schema.getDefault() != null) { // has default value
            if (cp.isArray) {
                List<String> _values = new ArrayList<>();

                if (schema.getDefault() instanceof ArrayNode) { // array of default values
                    ArrayNode _default = (ArrayNode) schema.getDefault();
                    if (_default.isEmpty()) { // e.g. default: []
                        return getDefaultCollectionType(schema, "");
                    }

                    List<String> final_values = _values;
                    _default.elements().forEachRemaining((element) -> {
                        final_values.add(element.asText());
                    });
                } else if (schema.getDefault() instanceof Collection) {
                    var _default = (Collection<Object>) schema.getDefault();
                    List<String> final_values = _values;
                    _default.forEach((element) -> {
                        final_values.add(String.valueOf(element));
                    });

                    if (_default != null && _default.isEmpty() && defaultToEmptyContainer) {
                        // e.g. [] with the option defaultToEmptyContainer enabled
                        return getDefaultCollectionType(schema, "");
                    }
                } else { // single value
                    _values = java.util.Collections.singletonList(String.valueOf(schema.getDefault()));
                }

                String defaultValue = "";

                if (cp.items.getIsEnumOrRef()) { // inline or ref enum
                    List<String> defaultValues = new ArrayList<>();
                    for (String _value : _values) {
                        defaultValues.add(cp.items.datatypeWithEnum + "." + toEnumVarName(_value, cp.items.dataType));
                    }
                    defaultValue = StringUtils.join(defaultValues, ", ");
                } else if (_values.size() > 0) {
                    if (cp.items.isString) { // array item is string
                        defaultValue = String.format(Locale.ROOT, "\"%s\"", StringUtils.join(_values, "\", \""));
                    } else if (cp.items.isNumeric) {
                        defaultValue = _values.stream()
                                .map(v -> {
                                    if ("BigInteger".equals(cp.items.dataType)) {
                                        return "new BigInteger(\"" + v + "\")";
                                    } else if ("BigDecimal".equals(cp.items.dataType)) {
                                        return "new BigDecimal(\"" + v + "\")";
                                    } else if (cp.items.isFloat) {
                                        return v + "f";
                                    } else {
                                        return v;
                                    }
                                })
                                .collect(Collectors.joining(", "));
                    } else { // array item is non-string, e.g. integer
                        defaultValue = StringUtils.join(_values, ", ");
                    }
                } else {
                    return getDefaultCollectionType(schema);
                }

                return getDefaultCollectionType(schema, defaultValue);
            }
            if (cp.isMap) { // map
                // TODO
                return null;
            } else {
                throw new RuntimeException("Error. Codegen Property must be array/set/map: " + cp);
            }
        } else {
            return null;
        }
    }

    @Override
    public String toDefaultValue(CodegenProperty cp, Schema schema) {
        schema = ModelUtils.getReferencedSchema(this.openAPI, schema);
        if (ModelUtils.isArraySchema(schema)) {
            if (defaultToEmptyContainer) {
                // if default to empty container option is set, respect the default values provided in the spec
                return toArrayDefaultValue(cp, schema);
            } else if (schema.getDefault() == null) {
                // nullable or containerDefaultToNull set to true
                if (cp.isNullable || containerDefaultToNull) {
                    return null;
                }
                return getDefaultCollectionType(schema);
            }
            return toArrayDefaultValue(cp, schema);
        } else if (ModelUtils.isMapSchema(schema) && !(ModelUtils.isComposedSchema(schema))) {
            if (schema.getProperties() != null && schema.getProperties().size() > 0) {
                // object is complex object with free-form additional properties
                if (schema.getDefault() != null) {
                    return super.toDefaultValue(schema);
                }
                return null;
            }

            if (defaultToEmptyContainer) {
                // respect the default values provided in the spec when the option is enabled
                if (schema.getDefault() != null) {
                    return String.format(Locale.ROOT, "new %s<>()",
                            instantiationTypes().getOrDefault("map", "HashMap"));
                } else {
                    return null;
                }
            }

            // nullable or containerDefaultToNull set to true
            if (cp.isNullable || containerDefaultToNull) {
                return null;
            }

            if (ModelUtils.getAdditionalProperties(schema) == null) {
                return null;
            }

            return String.format(Locale.ROOT, "new %s<>()",
                    instantiationTypes().getOrDefault("map", "HashMap"));
        } else if (ModelUtils.isIntegerSchema(schema)) {
            if (schema.getDefault() != null) {
                if (SchemaTypeUtil.INTEGER64_FORMAT.equals(schema.getFormat())) {
                    return schema.getDefault().toString() + "l";
                } else {
                    return schema.getDefault().toString();
                }
            }
            return null;
        } else if (ModelUtils.isNumberSchema(schema)) {
            if (schema.getDefault() != null) {
                if (SchemaTypeUtil.FLOAT_FORMAT.equals(schema.getFormat())) {
                    return schema.getDefault().toString() + "f";
                } else if (SchemaTypeUtil.DOUBLE_FORMAT.equals(schema.getFormat())) {
                    return schema.getDefault().toString() + "d";
                } else {
                    return "new BigDecimal(\"" + schema.getDefault().toString() + "\")";
                }
            }
            return null;
        } else if (ModelUtils.isBooleanSchema(schema)) {
            if (schema.getDefault() != null) {
                return schema.getDefault().toString();
            }
            return null;
        } else if (ModelUtils.isURISchema(schema)) {
            if (schema.getDefault() != null) {
                return "URI.create(\"" + escapeText(String.valueOf(schema.getDefault())) + "\")";
            }
            return null;
        } else if (ModelUtils.isStringSchema(schema)) {
            if (schema.getDefault() != null) {
                String _default;
                if (schema.getDefault() instanceof Date) {
                    if ("java8".equals(getDateLibrary())) {
                        Date date = (Date) schema.getDefault();
                        LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                        return String.format(Locale.ROOT, "LocalDate.parse(\"%s\")", localDate.toString());
                    } else {
                        return null;
                    }
                } else if (schema.getDefault() instanceof java.time.OffsetDateTime) {
                    if ("java8".equals(getDateLibrary())) {
                        return String.format(Locale.ROOT, "OffsetDateTime.parse(\"%s\", %s)",
                                ((java.time.OffsetDateTime) schema.getDefault()).atZoneSameInstant(ZoneId.systemDefault()),
                                "java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(java.time.ZoneId.systemDefault())");
                    } else {
                        return null;
                    }
                } else if (schema.getDefault() instanceof UUID) {
                    return "UUID.fromString(\"" + String.valueOf(schema.getDefault()) + "\")";
                } else {
                    _default = String.valueOf(schema.getDefault());
                }

                if (schema.getEnum() == null) {
                    return "\"" + escapeText(_default) + "\"";
                } else {
                    // convert to enum var name later in postProcessModels
                    return _default;
                }
            }
            return null;
        } else if (ModelUtils.isObjectSchema(schema)) {
            if (schema.getDefault() != null) {
                try {
                    StringBuilder stringBuilder = new StringBuilder();
                    stringBuilder.append("new " + cp.datatypeWithEnum + "()");
                    Map<String, Schema> propertySchemas = schema.getProperties();
                    if(propertySchemas != null) {
                        // With `parseOptions.setResolve(true)`, objects with 1 key-value pair are LinkedHashMap and objects with more than 1 are ObjectNode
                        // When not set, objects of any size are ObjectNode
                        ObjectMapper objectMapper = new ObjectMapper();
                        ObjectNode objectNode;
                        if(!(schema.getDefault() instanceof ObjectNode)) {
                            objectNode = objectMapper.valueToTree(schema.getDefault());
                        } else {
                            objectNode = (ObjectNode) schema.getDefault();

                        }
                        Set<Map.Entry<String, JsonNode>> defaultProperties = objectNode.properties();
                        for (Map.Entry<String, JsonNode> defaultProperty : defaultProperties) {
                            String key = defaultProperty.getKey();
                            JsonNode value = defaultProperty.getValue();
                            Schema propertySchema = propertySchemas.get(key);
                            if (!value.isValueNode() || propertySchema == null) { //Skip complex objects for now
                                continue;
                            }

                            String defaultPropertyExpression = null;
                            if(ModelUtils.isLongSchema(propertySchema)) {
                                defaultPropertyExpression = value.asText()+"l";
                            } else if(ModelUtils.isIntegerSchema(propertySchema)) {
                                defaultPropertyExpression = value.asText();
                            } else if(ModelUtils.isDoubleSchema(propertySchema)) {
                                defaultPropertyExpression = value.asText()+"d";
                            } else if(ModelUtils.isFloatSchema(propertySchema)) {
                                defaultPropertyExpression = value.asText()+"f";
                            } else if(ModelUtils.isNumberSchema(propertySchema)) {
                                defaultPropertyExpression = "new java.math.BigDecimal(\"" + value.asText() + "\")";
                            } else if(ModelUtils.isURISchema(propertySchema)) {
                                defaultPropertyExpression = "java.net.URI.create(\"" + escapeText(value.asText()) + "\")";
                            } else if(ModelUtils.isDateSchema(propertySchema)) {
                                if("java8".equals(getDateLibrary())) {
                                    defaultPropertyExpression = String.format(Locale.ROOT, "java.time.LocalDate.parse(\"%s\")", value.asText());
                                }
                            } else if(ModelUtils.isDateTimeSchema(propertySchema)) {
                                if("java8".equals(getDateLibrary())) {
                                    defaultPropertyExpression = String.format(Locale.ROOT, "java.time.OffsetDateTime.parse(\"%s\", %s)",
                                            value.asText(),
                                            "java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME.withZone(java.time.ZoneId.systemDefault())");
                                }
                            } else if(ModelUtils.isUUIDSchema(propertySchema)) {
                                defaultPropertyExpression = "java.util.UUID.fromString(\"" + value.asText() + "\")";
                            } else if(ModelUtils.isStringSchema(propertySchema)) {
                                defaultPropertyExpression = "\"" + value.asText() + "\"";
                            } else if(ModelUtils.isBooleanSchema(propertySchema)) {
                                defaultPropertyExpression = value.asText();
                            }
                            if(defaultPropertyExpression != null) {
                                stringBuilder
//                                        .append(System.lineSeparator())
                                        .append(".")
                                        .append(toVarName(key))
                                        .append("(").append(defaultPropertyExpression).append(")");
                            }
                        }
                    }
                    return stringBuilder.toString();
                } catch (ClassCastException e) {
                    LOGGER.error("Can't resolve default value: "+schema.getDefault(), e);
                    return null;
                }
            }
            return null;
        } else if (ModelUtils.isComposedSchema(schema)) {
            if (schema.getDefault() != null) {
                return super.toDefaultValue(schema);
            }
            return null;
        }

        return super.toDefaultValue(schema);
    }

    private String getDefaultCollectionType(Schema schema) {
        return getDefaultCollectionType(schema, null);
    }

    private String getDefaultCollectionType(Schema schema, String defaultValues) {
        String arrayFormat = "new %s<>(Arrays.asList(%s))";

        if (defaultToEmptyContainer) {
            // respect the default value in the spec
            if (defaultValues == null) { // default value not provided
                return null;
            } else if (defaultValues.isEmpty()) { // e.g. [] to indicates empty container
                arrayFormat = "new %s<>()";
                return getDefaultCollectionType(arrayFormat, defaultValues, ModelUtils.isSet(schema));
            } else { // default value not empty
                return getDefaultCollectionType(arrayFormat, defaultValues, ModelUtils.isSet(schema));
            }
        }

        if (defaultValues == null || defaultValues.isEmpty()) {
            // default to empty container even though default value is null
            // to respect default values provided in the spec, set the option `defaultToEmptyContainer` properly
            defaultValues = "";
            arrayFormat = "new %s<>()";
        }

        return getDefaultCollectionType(arrayFormat, defaultValues, ModelUtils.isSet(schema));
    }

    private String getDefaultCollectionType(String arrayFormat, String defaultValues, boolean isSet) {
        if (isSet) {
            return String.format(Locale.ROOT, arrayFormat,
                    instantiationTypes().getOrDefault("set", "LinkedHashSet"), defaultValues);
        }
        return String.format(Locale.ROOT, arrayFormat, instantiationTypes().getOrDefault("array", "ArrayList"), defaultValues);
    }

    @Override
    public String toDefaultParameterValue(final Schema<?> schema) {
        Object defaultValue = schema.get$ref() != null ? ModelUtils.getReferencedSchema(openAPI, schema).getDefault() : schema.getDefault();
        if (defaultValue == null) {
            return null;
        }
        if (defaultValue instanceof Date) {
            Date date = (Date) schema.getDefault();
            LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return localDate.toString();
        }
        if (ModelUtils.isArraySchema(schema)) {
            // swagger-parser parses the default value differently depending on whether it's in a referenced file or not.
            // cf. https://github.com/swagger-api/swagger-parser/issues/1958
            // ArrayList if in the referenced file, ArrayNode if not.
            if (defaultValue instanceof ArrayNode) {
                ArrayNode array = (ArrayNode) defaultValue;
                return StreamSupport.stream(array.spliterator(), false)
                        .map(JsonNode::toString)
                        // remove wrapper quotes
                        .map(item -> StringUtils.removeStart(item, "\""))
                        .map(item -> StringUtils.removeEnd(item, "\""))
                        .collect(Collectors.joining(","));
            } else if (defaultValue instanceof ArrayList) {
                ArrayList<?> array = (ArrayList<?>) defaultValue;
                return array.stream()
                        .map(Object::toString)
                        .collect(Collectors.joining(","));
            }
        }
        // escape quotes
        return defaultValue.toString().replace("\"", "\\\"");
    }

    /**
     * Return the example value of the parameter. Overrides the
     * setParameterExampleValue(CodegenParameter, Parameter) method in
     * DefaultCodegen to always call setParameterExampleValue(CodegenParameter)
     * in this class, which adds single quotes around strings from the
     * x-example property.
     *
     * @param codegenParameter Codegen parameter
     * @param parameter        Parameter
     */
    @Override
    public void setParameterExampleValue(CodegenParameter codegenParameter, Parameter parameter) {
        if (parameter.getExample() != null) {
            codegenParameter.example = parameter.getExample().toString();
        }

        if (parameter.getExamples() != null && !parameter.getExamples().isEmpty()) {
            Example example = parameter.getExamples().values().iterator().next();
            if (example.getValue() != null) {
                codegenParameter.example = example.getValue().toString();
            }
        }

        Schema schema = parameter.getSchema();
        if (schema != null && schema.getExample() != null) {
            codegenParameter.example = schema.getExample().toString();
        }

        setParameterExampleValue(codegenParameter);
    }

    /**
     * Return the example value of the parameter. Overrides the parent method in DefaultCodegen
     * to not set examples on complex models, as they don't compile properly.
     *
     * @param codegenParameter Codegen parameter
     * @param requestBody      Request body
     */
    @Override
    public void setParameterExampleValue(CodegenParameter codegenParameter, RequestBody requestBody) {
        boolean isModel = (codegenParameter.isModel || (codegenParameter.isContainer && codegenParameter.getItems().isModel));

        Content content = requestBody.getContent();

        if (content.size() > 1) {
            // @see ModelUtils.getSchemaFromContent()
            LOGGER.debug("Multiple MediaTypes found, using only the first one");
        }

        MediaType mediaType = content.values().iterator().next();
        if (mediaType.getExample() != null) {
            if (isModel) {
                once(LOGGER).warn("Ignoring complex example on request body");
            } else {
                codegenParameter.example = mediaType.getExample().toString();
                return;
            }
        }

        if (mediaType.getExamples() != null && !mediaType.getExamples().isEmpty()) {
            Example example = mediaType.getExamples().values().iterator().next();
            if (example.getValue() != null) {
                if (isModel) {
                    once(LOGGER).warn("Ignoring complex example on request body");
                } else {
                    codegenParameter.example = example.getValue().toString();
                    return;
                }
            }
        }

        setParameterExampleValue(codegenParameter);
    }

    @Override
    public void setParameterExampleValue(CodegenParameter p) {
        String example;

        boolean hasAllowableValues = p.allowableValues != null && !p.allowableValues.isEmpty();
        if (hasAllowableValues) {
            //support examples for inline enums
            final List<Object> values = (List<Object>) p.allowableValues.get("values");
            example = String.valueOf(values.get(0));
        } else if (p.defaultValue == null) {
            example = p.example;
        } else {
            example = p.defaultValue;
        }

        String type = p.baseType;
        if (type == null) {
            type = p.dataType;
        }

        if ("String".equals(type)) {
            if (example == null) {
                example = p.paramName + "_example";
            }
            example = "\"" + escapeText(example) + "\"";
        } else if ("Integer".equals(type) || "Short".equals(type)) {
            if (example == null) {
                example = "56";
            }
        } else if ("Long".equals(type)) {
            if (example == null) {
                example = "56";
            }
            example = StringUtils.appendIfMissingIgnoreCase(example, "L");
        } else if ("Float".equals(type)) {
            if (example == null) {
                example = "3.4";
            }
            example = StringUtils.appendIfMissingIgnoreCase(example, "F");
        } else if ("Double".equals(type)) {
            if (example == null) {
                example = "3.4";
            }
            example = StringUtils.appendIfMissingIgnoreCase(example, "D");
        } else if ("Boolean".equals(type)) {
            if (example == null) {
                example = "true";
            }
        } else if ("File".equals(type)) {
            if (example == null) {
                example = "/path/to/file";
            }
            example = "new File(\"" + escapeText(example) + "\")";
        } else if ("Date".equals(type)) {
            example = "new Date()";
        } else if ("LocalDate".equals(type)) {
            if (example == null) {
                example = "LocalDate.now()";
            } else {
                example = "LocalDate.parse(\"" + example + "\")";
            }
        } else if ("OffsetDateTime".equals(type)) {
            if (example == null) {
                example = "OffsetDateTime.now()";
            } else {
                example = "OffsetDateTime.parse(\"" + example + "\")";
            }
        } else if ("BigDecimal".equals(type)) {
            if (example == null) {
                example = "new BigDecimal(78)";
            } else {
                example = "new BigDecimal(\"" + example + "\")";
            }
        } else if ("UUID".equals(type)) {
            if (example == null) {
                example = "UUID.randomUUID()";
            } else {
                example = "UUID.fromString(\"" + example + "\")";
            }
        } else if (hasAllowableValues) {
            //parameter is enum defined as a schema component
            example = type + ".fromValue(\"" + example + "\")";
        } else if (!languageSpecificPrimitives.contains(type)) {
            // type is a model class, e.g. User
            example = "new " + type + "()";
        }

        if (example == null) {
            example = "null";
        } else if (Boolean.TRUE.equals(p.isArray)) {
            if (p.items != null && p.items.defaultValue != null) {
                String innerExample;
                if ("String".equals(p.items.dataType)) {
                    innerExample = "\"" + p.items.defaultValue + "\"";
                } else {
                    innerExample = p.items.defaultValue;
                }
                example = "Arrays.asList(" + innerExample + ")";
            } else {
                example = "Arrays.asList()";
            }
        } else if (Boolean.TRUE.equals(p.isMap)) {
            example = "new HashMap()";
        }

        p.example = example;
    }

    @Override
    public String toExampleValue(Schema p) {
        if (p.getExample() != null) {
            if (p.getExample() instanceof Date) {
                Date date = (Date) p.getExample();
                return DateTimeFormatter.ISO_LOCAL_DATE.format(ZonedDateTime.ofInstant(date.toInstant(), UTC));
            }
            return escapeText(p.getExample().toString());
        } else {
            return null;
        }
    }

    @Override
    public String getSchemaType(Schema p) {
        String openAPIType = super.getSchemaType(p);

        // don't apply renaming on types from the typeMapping
        if (typeMapping.containsKey(openAPIType)) {
            return typeMapping.get(openAPIType);
        }

        if (null == openAPIType) {
            LOGGER.error("No Type defined for Schema {}", p);
        }
        return toModelName(openAPIType);
    }

    @Override
    public String toOperationId(String operationId) {
        // throw exception if method name is empty
        if (StringUtils.isEmpty(operationId)) {
            throw new RuntimeException("Empty method/operation name (operationId) not allowed");
        }

        operationId = camelize(sanitizeName(operationId), LOWERCASE_FIRST_LETTER);

        // method name cannot use reserved keyword, e.g. return
        if (isReservedWord(operationId)) {
            String newOperationId = camelize("call_" + operationId, LOWERCASE_FIRST_LETTER);
            LOGGER.warn("{} (reserved word) cannot be used as method name. Renamed to {}", operationId, newOperationId);
            return newOperationId;
        }

        // operationId starts with a number
        if (operationId.matches("^\\d.*")) {
            LOGGER.warn(operationId + " (starting with a number) cannot be used as method name. Renamed to " + camelize("call_" + operationId), true);
            operationId = camelize("call_" + operationId, LOWERCASE_FIRST_LETTER);
        }

        return operationId;
    }

    @Override
    public CodegenModel fromModel(String name, Schema model) {
        Map<String, Schema> allDefinitions = ModelUtils.getSchemas(this.openAPI);
        CodegenModel codegenModel = super.fromModel(name, model);
        if (codegenModel.description != null) {
            if (!AnnotationLibrary.SWAGGER2.equals(getAnnotationLibrary())) {
                // TODO: should only be for SWAGGER1, but some NONE/MICROPROFILE templates still use it
                codegenModel.imports.add("ApiModel");
            }
        }
        if (codegenModel.discriminator != null && jackson) {
            codegenModel.imports.add("JsonSubTypes");
            codegenModel.imports.add("JsonTypeInfo");
            codegenModel.imports.add("JsonIgnoreProperties");
        }
        if (codegenModel.getIsClassnameSanitized() && jackson && !codegenModel.isEnum) {
            codegenModel.imports.add("JsonTypeName");
        }
        if (allDefinitions != null && codegenModel.parentSchema != null && codegenModel.hasEnums) {
            final Schema parentModel = allDefinitions.get(codegenModel.parentSchema);
            final CodegenModel parentCodegenModel = super.fromModel(codegenModel.parent, parentModel);
            codegenModel = AbstractJavaCodegen.reconcileInlineEnums(codegenModel, parentCodegenModel);
        }
        if ("BigDecimal".equals(codegenModel.dataType)) {
            codegenModel.imports.add("BigDecimal");
        }

        // additional import for different cases
        addAdditionalImports(codegenModel, codegenModel.getComposedSchemas());
        setEnumDiscriminatorDefaultValue(codegenModel);
        return codegenModel;
    }

    private void addAdditionalImports(CodegenModel model, CodegenComposedSchemas composedSchemas) {
        if (composedSchemas == null) {
            return;
        }

        final List<List<CodegenProperty>> propertyLists = Arrays.asList(
                composedSchemas.getAnyOf(),
                composedSchemas.getOneOf(),
                composedSchemas.getAllOf());
        for (final List<CodegenProperty> propertyList : propertyLists) {
            if (propertyList == null) {
                continue;
            }
            for (CodegenProperty cp : propertyList) {
                final String dataType = cp.baseType;
                if (null != importMapping().get(dataType)) {
                    model.imports.add(dataType);
                }
            }
        }
    }

    @Override
    public void postProcessModelProperty(CodegenModel model, CodegenProperty property) {
        if (serializeBigDecimalAsString && jackson) {
            if ("decimal".equals(property.baseType) || "bigdecimal".equalsIgnoreCase(property.baseType)) {
                // we serialize BigDecimal as `string` to avoid precision loss
                property.vendorExtensions.put("x-extra-annotation", "@JsonFormat(shape = JsonFormat.Shape.STRING)");

                // this requires some more imports to be added for this model...
                model.imports.add("JsonFormat");
            }
        }

        // hard-coded Arrays import in Java client as it has been removed from the templates
        if (this instanceof JavaClientCodegen &&
                ("jersey2".equals(library) ||
                        "jersey3".equals(library) ||
                        "native".equals(library) ||
                        "okhttp-gson".equals(library))) {
            model.imports.add("Arrays");
        }

        if ("array".equals(property.containerType)) {
            model.imports.add("ArrayList");
            model.imports.add("Arrays");
        } else if ("set".equals(property.containerType)) {
            model.imports.add("LinkedHashSet");
            if ((!openApiNullable || !property.isNullable) && jackson) { // cannot be wrapped to nullable
                model.imports.add("JsonDeserialize");
                property.vendorExtensions.put("x-setter-extra-annotation", "@JsonDeserialize(as = LinkedHashSet.class)");
            }
        } else if ("map".equals(property.containerType)) {
            model.imports.add("HashMap");
        }

        if (!BooleanUtils.toBoolean(model.isEnum)) {
            // needed by all pojos, but not enums
            if (!AnnotationLibrary.SWAGGER2.equals(getAnnotationLibrary())) {
                // TODO: should only be for SWAGGER1, but some NONE/MICROPROFILE templates still use it
                model.imports.add("ApiModelProperty");
                model.imports.add("ApiModel");
            }
        }

        if (openApiNullable) {
            if (Boolean.FALSE.equals(property.required) && Boolean.TRUE.equals(property.isNullable)) {
                model.imports.add("JsonNullable");
                model.getVendorExtensions().put("x-jackson-optional-nullable-helpers", true);
            }
        }

        if (property.isReadOnly) {
            model.getVendorExtensions().put("x-has-readonly-properties", true);
        }

        // if data type happens to be the same as the property name and both are upper case
        if (property.dataType != null && property.dataType.equals(property.name) && property.dataType.toUpperCase(Locale.ROOT).equals(property.name)) {
            property.name = property.name.toLowerCase(Locale.ROOT);
        }
    }

    @Override
    public void postProcessResponseWithProperty(CodegenResponse response, CodegenProperty property) {
        if (response == null || property == null || response.dataType == null || property.dataType == null) {
            return;
        }

        // the response data types should not contain bean validation annotations.
        property.dataType = removeAnnotations(property.dataType);
        response.dataType = removeAnnotations(response.dataType);
    }

    /**
     * Remove annotations from the given data type string.
     * <p>
     * For example:
     * <ul>
     *     <li>{@code @Min(0) @Max(10)Integer} -> {@code Integer}</li>
     *     <li>{@code @Pattern(regexp = "^[a-z]$")String>} -> {@code String}</li>
     *     <li>{@code List<@Pattern(regexp = "^[a-z]$")String>}" -> "{@code List<String>}"</li>
     *     <li>{@code List<@Valid Pet>}" -> "{@code List<Pet>}"</li>
     * </ul>
     *
     * @param dataType the data type string
     * @return the data type string without annotations
     */
    public String removeAnnotations(String dataType) {
        if (dataType != null && dataType.contains("@")) {
            return dataType.replaceAll("(?:(?i)@[a-z0-9]*+([(].*[)]|\\s*))*+", "");
        }
        return dataType;
    }

    /**
     * Sanitize the datatype.
     * This will remove all characters except alphanumeric ones.
     * It will also first use {{@link #removeAnnotations(String)}} to remove the annotations added to the datatype
     * @param dataType the data type string
     * @return the data type string without annotations and any characters except alphanumeric ones
     */
    public String sanitizeDataType(String dataType) {
        String content = removeAnnotations(dataType);
        if (content != null && content.matches(".*\\P{Alnum}.*")) {
            content = content.replaceAll("\\P{Alnum}", "");
        }
        return content;
    }


    @Override
    public ModelsMap postProcessModels(ModelsMap objs) {
        // recursively add import for mapping one type to multiple imports
        List<Map<String, String>> recursiveImports = objs.getImports();
        if (recursiveImports == null)
            return objs;

        ListIterator<Map<String, String>> listIterator = recursiveImports.listIterator();
        while (listIterator.hasNext()) {
            String _import = listIterator.next().get("import");
            // if the import package happens to be found in the importMapping (key)
            // add the corresponding import package to the list
            if (importMapping.containsKey(_import)) {
                Map<String, String> newImportMap = new HashMap<>();
                newImportMap.put("import", importMapping.get(_import));
                listIterator.add(newImportMap);
            }
        }

        // add x-implements for serializable to all models
        for (ModelMap mo : objs.getModels()) {
            CodegenModel cm = mo.getModel();
            if (this.serializableModel) {
                cm.getVendorExtensions().putIfAbsent("x-implements", new ArrayList<String>());
                ((ArrayList<String>) cm.getVendorExtensions().get("x-implements")).add("Serializable");
            }
        }

        // parse lombok additional model type annotations
        Map<String, Boolean> lombokOptions = new HashMap<>();
        String regexp = "@lombok.(\\w+\\.)*(?<ClassName>\\w+)(\\(.*?\\))?";
        Pattern pattern = Pattern.compile(regexp);
        for (String annotation : additionalModelTypeAnnotations) {
            Matcher matcher = pattern.matcher(annotation);
            if (matcher.find()) {
                String className = matcher.group("ClassName");
                lombokOptions.put(className, true);
            }
        }
        if (!lombokOptions.isEmpty()) {
            lombokAnnotations = lombokOptions;
            writePropertyBack(LOMBOK, lombokOptions);
        }

        return postProcessModelsEnum(objs);
    }

    @Override
    public OperationsMap postProcessOperationsWithModels(OperationsMap objs, List<ModelMap> allModels) {
        // Remove imports of List, ArrayList, Map and HashMap as they are
        // imported in the template already.
        List<Map<String, String>> imports = objs.getImports();
        Pattern pattern = Pattern.compile("java\\.util\\.(List|ArrayList|Map|HashMap)");
        for (Iterator<Map<String, String>> itr = imports.iterator(); itr.hasNext(); ) {
            String itrImport = itr.next().get("import");
            if (pattern.matcher(itrImport).matches()) {
                itr.remove();
            }
        }

        OperationMap operations = objs.getOperations();
        List<CodegenOperation> operationList = operations.getOperation();
        for (CodegenOperation op : operationList) {
            // check if the operation has form parameters
            if (op.getHasFormParams()) {
                additionalProperties.put("hasFormParamsInSpec", true);
            }
            Collection<String> operationImports = new ConcurrentSkipListSet<>();
            for (CodegenParameter p : op.allParams) {
                if (importMapping.containsKey(p.dataType)) {
                    operationImports.add(importMapping.get(p.dataType));
                }
            }
            op.vendorExtensions.put("x-java-import", operationImports);

            handleImplicitHeaders(op);
            handleConstantParams(op);
        }

        return objs;
    }

    @Override
    public void preprocessOpenAPI(OpenAPI openAPI) {
        super.preprocessOpenAPI(openAPI);
        if (openAPI == null) {
            return;
        }
        if (openAPI.getPaths() != null) {
            for (Map.Entry<String, PathItem> openAPIGetPathsEntry : openAPI.getPaths().entrySet()) {
                String pathname = openAPIGetPathsEntry.getKey();
                PathItem path = openAPIGetPathsEntry.getValue();
                if (path.readOperations() == null) {
                    continue;
                }
                for (Operation operation : path.readOperations()) {
                    LOGGER.info("Processing operation {}", operation.getOperationId());
                    if (hasBodyParameter(operation) || hasFormParameter(operation)) {
                        String defaultContentType = hasFormParameter(operation) ? "application/x-www-form-urlencoded" : "application/json";
                        List<String> consumes = new ArrayList<>(getConsumesInfo(openAPI, operation));
                        String contentType = consumes.isEmpty() ? defaultContentType : consumes.get(0);
                        operation.addExtension("x-content-type", contentType);
                    }
                    String[] accepts = getAccepts(openAPI, operation);
                    operation.addExtension("x-accepts", accepts);
                }
            }
        }

        // TODO: Setting additionalProperties is not the responsibility of this method. These side-effects should be moved elsewhere to prevent unexpected behaviors.
        if (artifactVersion == null) {
            // If no artifactVersion is provided in additional properties, version from API specification is used.
            // If none of them is provided then fallbacks to default version
            if (additionalProperties.containsKey(CodegenConstants.ARTIFACT_VERSION) && additionalProperties.get(CodegenConstants.ARTIFACT_VERSION) != null) {
                this.setArtifactVersion((String) additionalProperties.get(CodegenConstants.ARTIFACT_VERSION));
            } else if (openAPI.getInfo() != null && !StringUtils.isBlank(openAPI.getInfo().getVersion())) {
                this.setArtifactVersion(openAPI.getInfo().getVersion());
            } else {
                this.setArtifactVersion(ARTIFACT_VERSION_DEFAULT_VALUE);
            }
        }
        additionalProperties.put(CodegenConstants.ARTIFACT_VERSION, artifactVersion);

        if (additionalProperties.containsKey(CodegenConstants.SNAPSHOT_VERSION)) {
            if (convertPropertyToBooleanAndWriteBack(CodegenConstants.SNAPSHOT_VERSION)) {
                this.setArtifactVersion(this.buildSnapshotVersion(this.getArtifactVersion()));
            }
        }
        additionalProperties.put(CodegenConstants.ARTIFACT_VERSION, artifactVersion);

        if (ignoreAnyOfInEnum) {
            // Alter OpenAPI schemas ignore anyOf keyword if it consist of an enum. Example:
            //     anyOf:
            //     - type: string
            //       enum:
            //       - ENUM_A
            //       - ENUM_B
            Stream.concat(
                            Stream.of(openAPI.getComponents().getSchemas()),
                            openAPI.getComponents().getSchemas().values().stream()
                                    .filter(schema -> schema.getProperties() != null)
                                    .map(Schema::getProperties))
                    .forEach(schemas -> schemas.replaceAll(
                            (name, s) -> Stream.of(s)
                                    .filter(schema -> ModelUtils.isComposedSchema((Schema) schema))
                                    //.map(schema -> (ComposedSchema) schema)
                                    .filter(schema -> Objects.nonNull(((Schema) schema).getAnyOf()))
                                    .flatMap(schema -> ((Schema) schema).getAnyOf().stream())
                                    .filter(schema -> Objects.nonNull(((Schema) schema).getEnum()))
                                    .findFirst()
                                    .orElse((Schema) s)));
        }
    }

    private static String[] getAccepts(OpenAPI openAPIArg, Operation operation) {
        final Set<String> producesInfo = getProducesInfo(openAPIArg, operation);
        if (producesInfo != null && !producesInfo.isEmpty()) {
            return producesInfo.toArray(new String[]{});
        }
        return new String[]{"application/json"}; // default media type
    }

    @Override
    protected boolean needToImport(String type) {
        return super.needToImport(type) && !type.contains(".");
    }

    @Override
    public String toEnumName(CodegenProperty property) {
        return sanitizeName(camelize(property.name)) + "Enum";
    }

    private boolean isValidVariableNameInVersion(CharSequence name, SourceVersion version) {
        return SourceVersion.isIdentifier(name) && !SourceVersion.isKeyword(name, version);
    }

    @Override
    public String toEnumVarName(String value, String datatype) {
        if (enumNameMapping.containsKey(value)) {
            return enumNameMapping.get(value);
        }

        if (value.length() == 0) {
            return "EMPTY";
        }

        // for symbol, e.g. $, #
        if (getSymbolName(value) != null) {
            return getSymbolName(value).toUpperCase(Locale.ROOT);
        }

        if (" ".equals(value)) {
            return "SPACE";
        }

        // number
        if ("Integer".equals(datatype) || "Long".equals(datatype) ||
                "Float".equals(datatype) || "Double".equals(datatype) || "BigDecimal".equals(datatype)) {
            String varName = "NUMBER_" + value;
            varName = varName.replaceAll("-", "MINUS_");
            varName = varName.replaceAll("\\+", "PLUS_");
            varName = varName.replaceAll("\\.", "_DOT_");
            return varName;
        }

        // string
        String var;
        switch (getEnumPropertyNaming()) {
            case legacy:
                // legacy ,e.g. WITHNUMBER1
                var = value.replaceAll("\\W+", "_").toUpperCase(Locale.ROOT);
                break;
            case original:
                // keep value as it is, if meets language naming convention
                if (isValidVariableNameInVersion(value, SourceVersion.RELEASE_11)) {
                    return value;
                } else {
                    LOGGER.warn("Enum value '{}' is not a valid variable name in Java 11. Enum value will be renamed.", value);
                    var = value;
                }
                break;
            default:
                // default to MACRO_CASE, e.g. WITH_NUMBER1
                var = underscore(value.replaceAll("\\W+", "_")).toUpperCase(Locale.ROOT);
                break;
        }
        if (var.matches("\\d.*")) {
            var = "_" + var;
        }
        return this.toVarName(var);
    }

    @Override
    public String toEnumValue(String value, String datatype) {
        if ("Integer".equals(datatype) || "Double".equals(datatype)) {
            return value;
        } else if ("Long".equals(datatype)) {
            // add l to number, e.g. 2048 => 2048l
            return value + "l";
        } else if ("Float".equals(datatype)) {
            // add f to number, e.g. 3.14 => 3.14f
            return value + "f";
        } else if ("BigDecimal".equals(datatype)) {
            // use BigDecimal String constructor
            return "new BigDecimal(\"" + value + "\")";
        } else if ("URI".equals(datatype)) {
            return "URI.create(\"" + escapeText(value) + "\")";
        } else {
            return "\"" + escapeText(value) + "\"";
        }
    }

    @Override
    public CodegenOperation fromOperation(String path, String httpMethod, Operation operation, List<Server> servers) {
        CodegenOperation op = super.fromOperation(path, httpMethod, operation, servers);
        op.path = sanitizePath(op.path);
        return op;
    }

    private static CodegenModel reconcileInlineEnums(CodegenModel codegenModel, CodegenModel parentCodegenModel) {
        // This generator uses inline classes to define enums, which breaks when
        // dealing with models that have subTypes. To clean this up, we will analyze
        // the parent and child models, look for enums that match, and remove
        // them from the child models and leave them in the parent.
        // Because the child models extend the parents, the enums will be available via the parent.

        // Only bother with reconciliation if the parent model has enums.
        if (!parentCodegenModel.hasEnums) {
            return codegenModel;
        }

        // Get the properties for the parent and child models
        final List<CodegenProperty> parentModelCodegenProperties = parentCodegenModel.vars;
        List<CodegenProperty> codegenProperties = codegenModel.vars;

        // Iterate over all of the parent model properties
        boolean removedChildEnum = false;
        for (CodegenProperty parentModelCodegenProperty : parentModelCodegenProperties) {
            // Look for enums
            if (parentModelCodegenProperty.isEnum) {
                // Now that we have found an enum in the parent class,
                // and search the child class for the same enum.
                Iterator<CodegenProperty> iterator = codegenProperties.iterator();
                while (iterator.hasNext()) {
                    CodegenProperty codegenProperty = iterator.next();
                    if (codegenProperty.isEnum && codegenProperty.equals(parentModelCodegenProperty)) {
                        // We found an enum in the child class that is
                        // a duplicate of the one in the parent, so remove it.
                        iterator.remove();
                        removedChildEnum = true;
                    }
                }
            }
        }

        if (removedChildEnum) {
            codegenModel.vars = codegenProperties;
        }
        return codegenModel;
    }

    private static String sanitizePackageName(String packageName) {
        packageName = packageName.trim(); // FIXME: a parameter should not be assigned. Also declare the methods parameters as 'final'.
        packageName = packageName.replaceAll("[^a-zA-Z0-9_\\.]", "_");
        if (Strings.isNullOrEmpty(packageName)) {
            return "invalidPackageName";
        }
        return packageName;
    }

    private String sanitizePath(String p) {
        //prefer replace a ", instead of a fuLL URL encode for readability
        return p.replaceAll("\"", "%22");
    }

    @Override
    public void setOutputDir(String dir) {
        super.setOutputDir(dir);
        if (this.outputTestFolder.isEmpty()) {
            setOutputTestFolder(dir);
        }
    }

    public String getOutputTestFolder() {
        if (outputTestFolder.isEmpty()) {
            return DEFAULT_TEST_FOLDER;
        }
        return outputTestFolder;
    }

    @Override
    public DocumentationProvider getDocumentationProvider() {
        return documentationProvider;
    }

    @Override
    public void setDocumentationProvider(DocumentationProvider documentationProvider) {
        this.documentationProvider = documentationProvider;
    }

    @Override
    public AnnotationLibrary getAnnotationLibrary() {
        return annotationLibrary;
    }

    @Override
    public void setAnnotationLibrary(AnnotationLibrary annotationLibrary) {
        this.annotationLibrary = annotationLibrary;
    }

    @Override
    public String escapeQuotationMark(String input) {
        // remove " to avoid code injection
        return input.replace("\"", "");
    }

    @Override
    public String escapeUnsafeCharacters(String input) {
        return input.replace("*/", "*_/").replace("/*", "/_*");
    }

    /*
     * Derive invoker package name based on the input
     * e.g. foo.bar.model => foo.bar
     *
     * @param input API package/model name
     * @return Derived invoker package name based on API package/model name
     */
    private String deriveInvokerPackageName(String input) {
        String[] parts = input.split(Pattern.quote(".")); // Split on period.

        StringBuilder sb = new StringBuilder();
        String delim = "";
        for (String p : Arrays.copyOf(parts, parts.length - 1)) {
            sb.append(delim).append(p);
            delim = ".";
        }
        return sb.toString();
    }

    /**
     * Builds a SNAPSHOT version from a given version.
     *
     * @param version
     * @return SNAPSHOT version
     */
    private String buildSnapshotVersion(String version) {
        if (version.endsWith("-SNAPSHOT")) {
            return version;
        }
        return version + "-SNAPSHOT";
    }

    @Override
    public String toRegularExpression(String pattern) {
        return escapeText(pattern);
    }

    /**
     * Output the Getter name for boolean property, e.g. isActive
     *
     * @param name the name of the property
     * @return getter name based on naming convention
     */
    @Override
    public String toBooleanGetter(String name) {
        return booleanGetterPrefix + getterAndSetterCapitalize(name);
    }

    @Override
    public String sanitizeTag(String tag) {
        tag = camelize(underscore(sanitizeName(tag)));

        // tag starts with numbers
        if (tag.matches("^\\d.*")) {
            tag = "Class" + tag;
        }
        return tag;
    }

    /**
     * Camelize the method name of the getter and setter
     *
     * @param name string to be camelized
     * @return Camelized string
     */
    @Override
    public String getterAndSetterCapitalize(String name) {
        CamelizeOption camelizeOption = UPPERCASE_FIRST_CHAR;
        if (name == null || name.length() == 0) {
            return name;
        }
        name = toVarName(name);
        //
        // Let the property name capitalized
        // except when the first letter of the property name is lowercase and the second letter is uppercase
        // Refer to section 8.8: Capitalization of inferred names of the JavaBeans API specification
        // http://download.oracle.com/otn-pub/jcp/7224-javabeans-1.01-fr-spec-oth-JSpec/beans.101.pdf)
        //
        if (name.length() > 1 && Character.isLowerCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
            camelizeOption = LOWERCASE_FIRST_LETTER;
        }
        return camelize(name, camelizeOption);
    }

    @Override
    public void postProcessFile(File file, String fileType) {
        super.postProcessFile(file, fileType);
        if (file == null) {
            return;
        }

        String javaPostProcessFile = System.getenv("JAVA_POST_PROCESS_FILE");
        if (StringUtils.isEmpty(javaPostProcessFile)) {
            return; // skip if JAVA_POST_PROCESS_FILE env variable is not defined
        }

        // only process files with java extension
        if ("java".equals(FilenameUtils.getExtension(file.toString()))) {
            this.executePostProcessor(new String[]{javaPostProcessFile, file.toString()});
        }
    }

    @Override
    protected void addAdditionPropertiesToCodeGenModel(CodegenModel codegenModel, Schema schema) {
        if (!supportsAdditionalPropertiesWithComposedSchema) {
            // The additional (undeclared) properties are modeled in Java as a HashMap.
            //
            // 1. supportsAdditionalPropertiesWithComposedSchema is set to false:
            //    The generated model class extends from the HashMap. That does not work
            //    with composed schemas that also use a discriminator because the model class
            //    is supposed to extend from the generated parent model class.
            // 2. supportsAdditionalPropertiesWithComposedSchema is set to true:
            //    The HashMap is a field.
            super.addAdditionPropertiesToCodeGenModel(codegenModel, schema);
        }

        // See https://github.com/OpenAPITools/openapi-generator/pull/1729#issuecomment-449937728
        Schema s = ModelUtils.getAdditionalProperties(schema);
        // 's' may be null if 'additionalProperties: false' in the OpenAPI schema.
        if (s != null) {
            codegenModel.additionalPropertiesType = getSchemaType(s);
            addImport(codegenModel, codegenModel.additionalPropertiesType);
        }
    }

    /**
     * Search for property by {@link CodegenProperty#name}
     *
     * @param name       name to search for
     * @param properties list of properties
     * @return either found property or {@link Optional#empty()} if nothing has been found
     */
    protected Optional<CodegenProperty> findByName(String name, List<CodegenProperty> properties) {
        if (properties == null || properties.isEmpty()) {
            return Optional.empty();
        }

        return properties.stream()
                .filter(p -> p.name.equals(name))
                .findFirst();
    }

    /**
     * This method removes all implicit header parameters from the list of parameters
     *
     * @param operation - operation to be processed
     */
    protected void handleImplicitHeaders(CodegenOperation operation) {
        if (operation.allParams.isEmpty()) {
            return;
        }
        final ArrayList<CodegenParameter> copy = new ArrayList<>(operation.allParams);
        operation.allParams.clear();

        for (CodegenParameter p : copy) {
            if (p.isHeaderParam && (implicitHeaders || shouldBeImplicitHeader(p))) {
                operation.implicitHeadersParams.add(p);
                operation.headerParams.removeIf(header -> header.baseName.equals(p.baseName));
                LOGGER.info("Update operation [{}]. Remove header [{}] because it's marked to be implicit", operation.operationId, p.baseName);
            } else {
                operation.allParams.add(p);
            }
        }
    }

    private boolean shouldBeImplicitHeader(CodegenParameter parameter) {
        return StringUtils.isNotBlank(implicitHeadersRegex) && parameter.baseName.matches(implicitHeadersRegex);
    }

    @Override
    public void addImportsToOneOfInterface(List<Map<String, String>> imports) {
        if (jackson) {
            for (String i : Arrays.asList("JsonSubTypes", "JsonTypeInfo")) {
                Map<String, String> oneImport = new HashMap<>();
                oneImport.put("import", importMapping.get(i));
                if (!imports.contains(oneImport)) {
                    imports.add(oneImport);
                }
            }
        }
    }

    @Override
    public List<VendorExtension> getSupportedVendorExtensions() {
        List<VendorExtension> extensions = super.getSupportedVendorExtensions();
        extensions.add(VendorExtension.X_DISCRIMINATOR_VALUE);
        extensions.add(VendorExtension.X_IMPLEMENTS);
        extensions.add(VendorExtension.X_SETTER_EXTRA_ANNOTATION);
        extensions.add(VendorExtension.X_TAGS);
        extensions.add(VendorExtension.X_ACCEPTS);
        extensions.add(VendorExtension.X_CONTENT_TYPE);
        extensions.add(VendorExtension.X_CLASS_EXTRA_ANNOTATION);
        extensions.add(VendorExtension.X_FIELD_EXTRA_ANNOTATION);
        return extensions;
    }

    public boolean isAddNullableImports(CodegenModel cm, boolean addImports, CodegenProperty var) {
        if (this.openApiNullable) {
            boolean isOptionalNullable = Boolean.FALSE.equals(var.required) && Boolean.TRUE.equals(var.isNullable);
            // only add JsonNullable and related imports to optional and nullable values
            addImports |= isOptionalNullable;
            var.getVendorExtensions().put("x-is-jackson-optional-nullable", isOptionalNullable);
            findByName(var.name, cm.readOnlyVars)
                    .ifPresent(p -> p.getVendorExtensions().put("x-is-jackson-optional-nullable", isOptionalNullable));
        }
        return addImports;
    }

    public static void addImports(List<Map<String, String>> imports, CodegenModel cm, Map<String, String> imports2Classnames) {
        for (Map.Entry<String, String> entry : imports2Classnames.entrySet()) {
            cm.imports.add(entry.getKey());
            Map<String, String> importsItem = new HashMap<>();
            importsItem.put("import", entry.getValue());
            imports.add(importsItem);
        }
    }

    @Override
    public boolean isTypeErasedGenerics() {
        return true;
    }

    /**
     * Sets the naming convention for Java enum properties
     *
     * @param enumPropertyNamingType The string representation of the naming convention, as defined by {@link ENUM_PROPERTY_NAMING_TYPE}
     */
    public void setEnumPropertyNaming(final String enumPropertyNamingType) {
        try {
            this.enumPropertyNaming = ENUM_PROPERTY_NAMING_TYPE.valueOf(enumPropertyNamingType);
        } catch (IllegalArgumentException ex) {
            StringBuilder sb = new StringBuilder(enumPropertyNamingType + " is an invalid enum property naming option. Please choose from:");
            for (ENUM_PROPERTY_NAMING_TYPE t : ENUM_PROPERTY_NAMING_TYPE.values()) {
                sb.append("\n  ").append(t.name());
            }
            throw new RuntimeException(sb.toString());
        }
    }
}

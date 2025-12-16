# MicroProfile Language Server Changelog

## [0.16.0](https://github.com/eclipse-lsp4mp/lsp4mp/milestone/17?closed=1) (16 December, 2025)

### Enhancements
 * Add inlay hints for property type and property value converter used to properties files, and add inlay hint for default value in Java files. See [#529](https://github.com/eclipse-lsp4mp/lsp4mp/issues/529).

### Bug Fixes
 * Fix validating property values using user-provided custom converters. See [#532](https://github.com/eclipse-lsp4mp/lsp4mp/issues/532).

## [0.15.0](https://github.com/eclipse-lsp4mp/lsp4mp/milestone/16?closed=1) (5 December, 2025)

### Enhancements
 * Validate config property values using the microprofile-config runtime used by the project. See [#511](https://github.com/eclipse-lsp4mp/lsp4mp/issues/511), [#518](https://github.com/eclipse-lsp4mp/lsp4mp/pull/518), [#517](https://github.com/eclipse-lsp4mp/lsp4mp/pull/517).
 * Improve validation of @ConfigProperty/defaultValue for lists and arrays. See [#409](https://github.com/eclipse-lsp4mp/lsp4mp/issues/409).

### Bug Fixes
 * Recognise some mapped properties that weren't recognised before. See [#513](https://github.com/eclipse-lsp4mp/lsp4mp/pull/513).
 * Address `IllegalArgumentException` thrown in JavaCursorContextKind. See [#502](https://github.com/eclipse-lsp4mp/lsp4mp/pull/502).
 * Properly validate property values for properties of type `jakarta.inject.Provider`. See [#459](https://github.com/eclipse-lsp4mp/lsp4mp/issues/459).
 * Use '$' instead of '.' internally for fully qualified naming for classes. See [#521](https://github.com/eclipse-lsp4mp/lsp4mp/pull/521).
 * Use '$' instead of '.' internally for fully qualified naming for methods. See [#522](https://github.com/eclipse-lsp4mp/lsp4mp/pull/522).

## [0.14.0](https://github.com/eclipse-lsp4mp/lsp4mp/milestone/15?closed=1) (22 April, 2025)

### Performance
 * Skip making a fake project if only sources are being scanned. See [#494](https://github.com/eclipse-lsp4mp/lsp4mp/pull/494).

### Bug Fixes
 * Do not suggest method or field snippets when the cursor is outside the top level type declaration. See [#476](https://github.com/eclipse-lsp4mp/lsp4mp/pull/476).
 * Guard against annotation binding failing to resolve. See [#492](https://github.com/eclipse-lsp4mp/lsp4mp/pull/492).
 * Fix cases where the JDT component would sometimes fail to find an inner class. See [#479](https://github.com/eclipse-lsp4mp/lsp4mp/pull/479), [#480](https://github.com/eclipse-lsp4mp/lsp4mp/pull/480).

### Build
 * Improve stability and performance of tests. See [#474](https://github.com/eclipse-lsp4mp/lsp4mp/pull/474), [#487](https://github.com/eclipse-lsp4mp/lsp4mp/pull/487).

### New Contributors
Thanks to Paul Gooderham for his first contribution on the project!

## [0.13.0](https://github.com/eclipse/lsp4mp/milestone/14?closed=1) (October 22, 2024)

### Enhancements

- Provide property validator extension API. See [#460](https://github.com/eclipse/lsp4mp/pull/460).

### Bug Fixes

- Codelens for REST endpoints should resolve variable references. See [#467](https://github.com/eclipse/lsp4mp/pull/467).
- Prevent duplicate properties when generating them. See [#465](https://github.com/eclipse/lsp4mp/pull/465).
- Split register completion options for Java / Properties. See [#464](https://github.com/eclipse/lsp4mp/pull/464).
- Prevent error on empty name symbol. See [#462](https://github.com/eclipse/lsp4mp/pull/462).
- Check that document is not out of date when getText is consumed. See [#466](https://github.com/eclipse/lsp4mp/pull/466).

## [0.12.0](https://github.com/eclipse/lsp4mp/milestone/13?closed=1) (August 26, 2024)

### Enhancements

- Resolve system properties/environment variables while browsing the application.properties values. See [#448](https://github.com/eclipse/lsp4mp/issues/448).
- Add support for @Startup healthcheck diagnostic. See [#443](https://github.com/eclipse/lsp4mp/issues/443).

### Bug Fixes

- NPE with Workspace Symbol when LSP client return null as SymbolInformation List. See [#457](https://github.com/eclipse/lsp4mp/pull/457).
- Unrecognized property mp.messaging.* when Channel annotation is used along with Multi. See [#451](https://github.com/eclipse/lsp4mp/pull/451).
- fix: synchronized projectLabels + collect codeLens when project is loaded. See [#444](https://github.com/eclipse/lsp4mp/pull/444).

### Build

- Update eclipse.jdt.ls to 1.39.0-SNAPSHOT. See [#454](https://github.com/eclipse/lsp4mp/pull/454).
- Add About files to JDT extension bundles & Maven artifacts. See [#437](https://github.com/eclipse/lsp4mp/pull/437).

## [0.11.0](https://github.com/eclipse/lsp4mp/milestone/12?closed=1) (February 1, 2024)

### Enhancements
 * Delegate kotlin file (*.kt) to java document service. See [#430](https://github.com/eclipse/lsp4mp/pull/430).

### Bug Fixes
 * Don't generate an empty symbol name when property starts with '.'. See [#433](https://github.com/eclipse/lsp4mp/pull/433).

### Build
 * Use newer JDT.LS's JavadocContentAccess2 methods. See [432](https://github.com/eclipse/lsp4mp/pull/432).

## [0.10.0](https://github.com/eclipse/lsp4mp/milestone/11?closed=1) (October 5, 2023)

### Bug Fixes

 * Properly handle `StackOverflowError` in `MicroProfileDelegateCommandHandler.getMicroProfileProjectInfo`. See [#418](https://github.com/eclipse/lsp4mp/issues/418).
 * Fix `NullPointerException` in `TestJaxRsInfoProvider.canProvideJaxRsMethodInfoForClass`. See [#424](https://github.com/eclipse/lsp4mp/issues/424).
 * Fix `canProvideJaxRsMethodInfoForClass` for default JAX-RS. See [#420](https://github.com/eclipse/lsp4mp/pull/420).

### Build

 * Update to Tycho 3.0.5 and Maven 3.9.4. See [#422](https://github.com/eclipse/lsp4mp/pull/422), [#423](https://github.com/eclipse/lsp4mp/pull/423).
 * Update eclipse.jdt.ls to 1.29.0-SNAPSHOT. See [#427](https://github.com/eclipse/lsp4mp/pull/427).

## [0.9.0](https://github.com/eclipse/lsp4mp/milestone/10?closed=1) (August 8, 2023)

### Enhancements

 * Improve completion performance of `microprofile-config.properties` (remove unnecessary parameters in response). See [#410](https://github.com/eclipse/lsp4mp/issues/410).

### Bug Fixes

 * Add null check for data field in `completionItem/resolve` request. See [#412](https://github.com/eclipse/lsp4mp/pull/412).

### Build

 * Update eclipse.jdt.ls to `1.27.0-SNAPSHOT`. See [#415](https://github.com/eclipse/lsp4mp/pull/415).

## [0.8.0](https://github.com/eclipse/lsp4mp/milestone/9?closed=1) (June 15, 2023)

### Enhancements
 * Improve completion performance in properties files (resolve support, and item defaults). See [#389](https://github.com/eclipse/lsp4mp/issues/389).
 * Warning for type declarations that are incorrectly annotated while their methods are annotated with `@Query`/`@Mutation`. See [#355](https://github.com/eclipse/lsp4mp/issues/355).

### Bug Fixes
 * Don't implement by default custom language client API. See [#398](https://github.com/eclipse/lsp4mp/issues/398).
 * Make `javaCursorContext` calls consistent with all `PropertiesManagerForJava` calls. See [#390](https://github.com/eclipse/lsp4mp/issues/390).
 * Fix missing export packages for tests. See [#391](https://github.com/eclipse/lsp4mp/issues/391).

### Build
 * Update eclipse.jdt.ls to 1.24.0-SNAPSHOT. See [#401](https://github.com/eclipse/lsp4mp/pull/401).

## [0.7.1](https://github.com/eclipse/lsp4mp/milestone/8?closed=1) (April 12, 2023)

### Bug Fixes
 * Fix context-aware snippets in projects that use Project Lombok. See [#381](https://github.com/eclipse/lsp4mp/pull/381).

## [0.7.0](https://github.com/eclipse/lsp4mp/milestone/7?closed=1) (April 3, 2023)

### Enhancements
 * Validation for GraphQL `@Query` and `@Mutation` methods with `void` return type. See [#348](https://github.com/eclipse/lsp4mp/issues/348), [#359](https://github.com/eclipse/lsp4mp/issues/359).
 * Navigate to REST endpoints using workspace symbols. See [#87](https://github.com/eclipse/lsp4mp/issues/87).
 * Show config property documentation when hovering over the key in a properties file. See [#321](https://github.com/eclipse/lsp4mp/issues/321).
 * Validate lists in `@ConfigProperty`'s `defaultValue`. See [#351](https://github.com/eclipse/lsp4mp/pull/351).
 * Make Java file snippets context-aware. See [#108](https://github.com/eclipse/lsp4mp/issues/108).
 * Add an `id` to track the type of each `CodeAction`. See [#371](https://github.com/eclipse/lsp4mp/pull/371).

### Bug Fixes
 * Hover fails in properties files when the Java language server is loading. See [#375](https://github.com/eclipse/lsp4mp/issues/375).
 * Definition sometimes fails on property values in a properties file. See [#374](https://github.com/eclipse/lsp4mp/issues/374).
 * Adjust go to definition range for property keys to include the offset between the property key and `=`. See [#335](https://github.com/eclipse/lsp4mp/pull/335).
 * Fix `NullPointerException` during go to definition in properties files. See [#372](https://github.com/eclipse/lsp4mp/issues/372).
 * Fix `NullPointerException` on shutdown when LSP client doesn't define extendedClientCapabilities. See [#363](https://github.com/eclipse/lsp4mp/pull/363).
 * Completion causes Exceptions when typing in a Java file. See [#347](https://github.com/eclipse/lsp4mp/issues/347).
 * Support the `jakarta` namespace (JakartaEE 9+). See [#344](https://github.com/eclipse/lsp4mp/issues/344).
 * Hovering over properties file fails with `NullPointerException` when there are multiple definitions of a property. See [#341](https://github.com/eclipse/lsp4mp/issues/341).
 * `config_ordinal` appears as a property even in non-MicroProfile projects. See [#312](https://github.com/eclipse/lsp4mp/issues/312).
 * Quick fix to assign a value to a property now handles the prefix set by `@ConfigProperties` properly. See [#303](https://github.com/eclipse/lsp4mp/issues/303).
 * Change wording of "Unknown property" error message to "Unrecognized property". See [#290](https://github.com/eclipse/lsp4mp/issues/290).

### Build
 * Fix test error in `BasePropertiesManagerTest#createParentFolders`. See [#369](https://github.com/eclipse/lsp4mp/issues/369).

## [0.6.0](https://github.com/eclipse/lsp4mp/milestone/6?closed=1) (December 1, 2022)

### Enhancements
 * Display property value as inlay hint. See [#226](https://github.com/eclipse/lsp4mp/issues/226).
 * Property evaluation should support the environment variable default value notation. See [#241](https://github.com/eclipse/lsp4mp/issues/241).
 * Manage static properties using a `staticProvider` extension point. See [#44](https://github.com/eclipse/lsp4mp/issues/44).
 * Improve code action performance with `CodeAction#data` & `resolveCodeAction`. See [#171](https://github.com/eclipse/lsp4mp/issues/171).
 * Diagnostics for mp-reactive-messaging `@Incoming`/`@Outgoing` annotation. See [#58](https://github.com/eclipse/lsp4mp/issues/58).

### Bug Fixes
 * Java source code not validated upon start. See [#301](https://github.com/eclipse/lsp4mp/issues/301).
 * `ClassCastException` thrown (and caught) when using invalid `@ConfigProperty` default value. See [#295](https://github.com/eclipse/lsp4mp/issues/295).
 * Improve handling of `@ConfigProperties` for validation. See [#304](https://github.com/eclipse/lsp4mp/issues/304).
 * Support for the `config_ordinal` property in `microprofile-config.properties`. See [#289](https://github.com/eclipse/lsp4mp/issues/289).
 * Display property value when hovering over a key that isn't defined in the application. See [#285](https://github.com/eclipse/lsp4mp/issues/285).
 * REST client code lens only shows up for `GET` annotations. See [#94](https://github.com/eclipse/lsp4mp/issues/94).
 * JAXRS code lens URL should always appear above method declaration. See [#194](https://github.com/eclipse/lsp4mp/issues/194).
 * Support `microprofile-health` 3.0 and later. See [#314](https://github.com/eclipse/lsp4mp/issues/314).
 * Fix inlay hints & definitions when project returns empty properties. See [#311](https://github.com/eclipse/lsp4mp/pull/311).
 * Fix code lens when no configuration sources available. See [#315](https://github.com/eclipse/lsp4mp/issues/315).
 * `@ConfigProperties` validation should check the annotation's fully qualified name. See [#304](https://github.com/eclipse/lsp4mp/issues/304).
 * Fix typo in `mpirc` snippet. See [#325](https://github.com/eclipse/lsp4mp/issues/325).

### Build
 * Update Target Platform to 1.16.0-SNAPSHOT version of JDT-LS target. See [#288](https://github.com/eclipse/lsp4mp/pull/288).
 * JDT.LS dependency on tests should be optional. See [#286](https://github.com/eclipse/lsp4mp/issues/286).
 * Copy over `ModelTextDocuments#computeModelAsyncCompose` from quarkus-ls into commons package. See [#257](https://github.com/eclipse/lsp4mp/issues/257).
 * Move VS Code workspace configuration into correct folder. See [#145](https://github.com/eclipse/lsp4mp/pull/145).

## [0.5.0](https://github.com/eclipse/lsp4mp/milestone/5?closed=1) (July 25, 2022)

### Enhancements
 * Delay revalidation and handle validation cancellation correctly. See [#252](https://github.com/eclipse/lsp4mp/pull/252).
 * Property file with property expressions (without default value) are flagged as wrong. See [#225](https://github.com/eclipse/lsp4mp/issues/225), [#227](https://github.com/eclipse/lsp4mp/issues/227).

### Bug Fixes
 * Language Server attempts to calculate code actions for stale diagnostics. See [#272](https://github.com/eclipse/lsp4mp/issues/272).
 * Hovering property value fails with NPE. See [#265](https://github.com/eclipse/lsp4mp/issues/265).
 * Completing property name with existing value will replace current value with default value. See [#264](https://github.com/eclipse/lsp4mp/issues/264).
 * Empty completion when completion is triggered before the assign `=`. See [#255](https://github.com/eclipse/lsp4mp/issues/255).
 * Improve validation by handling some known corner cases. [#249](https://github.com/eclipse/lsp4mp/issues/249), [#235](https://github.com/eclipse/lsp4mp/issues/235), [#233](https://github.com/eclipse/lsp4mp/issues/233), [#232](https://github.com/eclipse/lsp4mp/issues/232), [#228](https://github.com/eclipse/lsp4mp/issues/228).
 * Improved MicroProfile property value expression diagnostic message. See [#242](https://github.com/eclipse/lsp4mp/pull/242).
 * Update quarkus-spring-web so that it is not vulnerable to CVE-2022-22965. See [#238](https://github.com/eclipse/lsp4mp/issues/238).
 * `javaASTValidator` schema refers to the wrong interface name. See [#234](https://github.com/eclipse/lsp4mp/issues/234).
 * `PropertyReplacerStrategy` is in wrong package. See [#239](https://github.com/eclipse/lsp4mp/issues/239).
 * Added `JDTTypeUtils.isVoidReturnType` for `QuarkusConfigMappingProvider` void check. See [#246](https://github.com/eclipse/lsp4mp/pull/246).
 * Rename profile parameter to ordinal for better readability. See [#236](https://github.com/eclipse/lsp4mp/issues/236).
 * `plugin.xml` not part of the test bundle. See [#230](https://github.com/eclipse/lsp4mp/issues/230).

### Build
 * Use JDT-LS 1.13.0 and build with Java 17. See [#266](https://github.com/eclipse/lsp4mp/issues/266).
 * Move to LSP4j 0.14.0. See [#254](https://github.com/eclipse/lsp4mp/issues/254).
 * Remove the m2e lifecycle mapping plugin from MANIFEST.MF. See [#269](https://github.com/eclipse/lsp4mp/pull/269).
 * Remove unnecessary 2019-06 release repository from target platform. See [#251](https://github.com/eclipse/lsp4mp/pull/251).
 * Remove unnecessary Gson dependency in pom file. See [#274](https://github.com/eclipse/lsp4mp/pull/274).

## [0.4.0](https://github.com/eclipse/lsp4mp/milestone/4?closed=1) (March 24, 2022)

### Enhancements
 * Support validation and code actions for `@ConfigProperty`. See [#90](https://github.com/eclipse/lsp4mp/issues/90), [#176](https://github.com/eclipse/lsp4mp/issues/176), [#147](https://github.com/eclipse/lsp4mp/issues/147).
 * Completion for properties defined using `@ConfigProperties`. See [#80](https://github.com/eclipse/lsp4mp/issues/80).
 * Support validation for `@Retry` annotation and its member values. See [#191](https://github.com/eclipse/lsp4mp/pull/191), [#196](https://github.com/eclipse/lsp4mp/issues/196).
 * Diagnostics for `@Asynchronous`, `@Bulkhead` & `@Timeout` annotations. See [#74](https://github.com/eclipse/lsp4mp/issues/74), [#184](https://github.com/eclipse/lsp4mp/pull/184), [#185](https://github.com/eclipse/lsp4mp/pull/185).
 * Support the `@ApplicationPath` annotation to handle the project URL. See [#179](https://github.com/eclipse/lsp4mp/issues/179).
 * Diagnostics for invalid annotation parameter values. See [#77](https://github.com/eclipse/lsp4mp/issues/77).
 * Reference only property declared in properties file in property expression. See [#205](https://github.com/eclipse/lsp4mp/issues/205).
 * Support for default value inside properties expression. See [#201](https://github.com/eclipse/lsp4mp/issues/201).
 * Exclude unassigned property with code action. See [#187](https://github.com/eclipse/lsp4mp/pull/187).
 * Binary dynamic properties should be generated after an update. See [#159](https://github.com/eclipse/lsp4mp/pull/159).
 * Support for config profiles. See [#78](https://github.com/eclipse/lsp4mp/issues/78).

### Bug Fixes
 * Provide API to configure root path of JAX RS resources. See [#174](https://github.com/eclipse/lsp4mp/pull/174).
 * projectLabels command doesn't support `jdt://` URIs. See [#220](https://github.com/eclipse/lsp4mp/issues/220).
 * Fix bug with missing definition hover for multiple annotation members. See [#216](https://github.com/eclipse/lsp4mp/pull/216).
 * Support optional property reference hover for annotation members. See [#211](https://github.com/eclipse/lsp4mp/pull/211).
 * Do not rebuild list of configuration properties when MicroProfile config sources are updated in the build directory. See [#162](https://github.com/eclipse/lsp4mp/issues/162).
 * Deadlock when client is sending burst of request. See [#177](https://github.com/eclipse/lsp4mp/issues/177).
 * Exclude the method that's being annotated when showing completion for fallback method. See [#148](https://github.com/eclipse/lsp4mp/issues/148).
 * SingleMemberAnnotation diagnostics not supported by annotationValidator. See [#188](https://github.com/eclipse/lsp4mp/issues/188).
 * Add 'shouldLanguageServerExitOnShutdown' to ExtendedClientCapabilities. See [#172](https://github.com/eclipse/lsp4mp/pull/172).
 * Fix class cast exception. See [#182](https://github.com/eclipse/lsp4mp/pull/182).

### Build
 * Update o.e.jdt.ls.tp dependency to 1.7.0 Release. See [#217](https://github.com/eclipse/lsp4mp/pull/217).
 * Update eclipse-jarsigner-plugin. See [#157](https://github.com/eclipse/lsp4mp/pull/157).

### Other
 * Move `@ApplicationPath` SearchEngine to JaxRsContext. See [#208](https://github.com/eclipse/lsp4mp/issues/208).
 * MicroProfileMetricsDiagnosticsParticipant difficult to read. See [#199](https://github.com/eclipse/lsp4mp/issues/199).
 * Remove QuarkusConfigSourceProvider. See [#160](https://github.com/eclipse/lsp4mp/pull/160).
 * Extension point for contributing configuration sources. See [#149](https://github.com/eclipse/lsp4mp/pull/149).

## [0.3.0](https://github.com/eclipse/lsp4mp/milestone/3?closed=1) (July 15, 2021)

### Enhancements

 * Completion for `fallbackMethod` in `@Fallback` annotation. See [#34](https://github.com/eclipse/lsp4mp/issues/34).

### Other

 * Remove pack200 support. See [#134](https://github.com/eclipse/lsp4mp/pull/134).

## [0.2.0](https://github.com/eclipse/lsp4mp/milestone/2?closed=1) (March 31, 2021)

### Enhancements

 * Support arbitrary number of member values in `PropertiesHoverParticipant`. See [#124](https://github.com/eclipse/lsp4mp/pull/124).
 * Add extension point to contribute properties to exclude from validation. See [#95](https://github.com/eclipse/lsp4mp/issues/95).
 * Definition support from Java to properties for `ConfigProperty/name`. See [#88](https://github.com/eclipse/lsp4mp/issues/88).
 * Automatically infer package names when inserting class snippets. See [#60](https://github.com/eclipse/lsp4mp/issues/60).
 * Support `handle-as` for metadata properties. See [#39](https://github.com/eclipse/lsp4mp/issues/39).
 * Display the different values for the different profiles in Java `@ConfigProperty` Hover. See [#98](https://github.com/eclipse/lsp4mp/issues/98).

### Bug Fixes

 * Trailing tab causes infinite loop in parser. See [#112](https://github.com/eclipse/lsp4mp/issues/112).
 * Prevent NPEs when working with MP 4.0 features. See [#119](https://github.com/eclipse/lsp4mp/issues/119).
 * Enhance the error message when out of bounds is detected. See [#114](https://github.com/eclipse/lsp4mp/pull/114).
 * Use `kill -0` instead of `ps -p` in `ParentProcessWatcher`. See [#110](https://github.com/eclipse/lsp4mp/issues/110).
 * Move quarkus test project names to quarkus-ls repo. See [#7](https://github.com/eclipse/lsp4mp/issues/7).
 * Wrong/Missing Log Levels in property files. See [#15](https://github.com/eclipse/lsp4mp/pull/105).
 * `mp.messaging` properties now work for Emitters. See [#127](https://github.com/eclipse/lsp4mp/pull/127).

### Other

 * Move `MicroProfileForJavaAssert` into `internal.core` class. See [#125](https://github.com/eclipse/lsp4mp/pull/125).
 * Sequence diagram of properties completion. See [#75](https://github.com/eclipse/lsp4mp/issues/75).
 * Fix Tools for MP name in sequence diagrams. See [#93](https://github.com/eclipse/lsp4mp/pull/93).

## [0.1.0](https://github.com/eclipse/lsp4mp/milestone/1?closed=1) (September 21, 2020)

### Enhancements

 * Java snippets for `microprofile rest client`. See [#55](https://github.com/eclipse/lsp4mp/issues/55).
 * Filter out duplicate completion items coming from MP implementations. See [#48](https://github.com/eclipse/lsp4mp/issues/48).
 * CDI scope diagnostics for `mp metrics @Gauge`. See [#46](https://github.com/eclipse/lsp4mp/issues/46).
 * Highlight support for property expression. See [#40](https://github.com/eclipse/lsp4mp/issues/40).
 * Diagnostics for ` mp-fault-tolerance fallbackMethod` . See [#33](https://github.com/eclipse/lsp4mp/issues/33).
 * Java `snippets for jax-rs`. See [#31](https://github.com/eclipse/lsp4mp/issues/31).
 * Snippets for new `microprofile health liveness / readiness checks`. See [#28](https://github.com/eclipse/lsp4mp/issues/28).
 * Properties support for `microprofile-graphql`. See [#27](https://github.com/eclipse/lsp4mp/issues/27).
 * Properties support for `microprofile-reactive-messaging`. See [#26](https://github.com/eclipse/lsp4mp/issues/26).
 * Hover for Property Expressions. See [#24](https://github.com/eclipse/lsp4mp/issues/24).
 * Properties support for microprofile-jwt-auth. See [#23](https://github.com/eclipse/lsp4mp/issues/23).
 * Property expression validation. See [#21](https://github.com/eclipse/lsp4mp/pull/21).
 * Property expression definition. See [#19](https://github.com/eclipse/lsp4mp/pull/19).
 * Hardcoded support for boolean converter. See [#17](https://github.com/eclipse/lsp4mp/pull/17).
 * Properties support for `microprofile-health`. See [#16](https://github.com/eclipse/lsp4mp/issues/16).
 * Model and completion for property expressions. See [#13](https://github.com/eclipse/lsp4mp/pull/13).

### Bug Fixes

 * NullPointerException with symbols. See [#66](https://github.com/eclipse/lsp4mp/issues/66).
 * Fix duplicate of `quarkus-properties` when registering `textDocument/rangeFormatting`. See [#52](https://github.com/eclipse/lsp4mp/pull/52).
 * Rename settings prefix to microprofile. See [#51](https://github.com/eclipse/lsp4mp/pull/51).
 * Fix missing unit in Gauge metrics snippet. See [#47](https://github.com/eclipse/lsp4mp/pull/47).
 * Escape special characters within LSP snippets. See [#29](https://github.com/eclipse/lsp4mp/pull/29).
 * Completion in properties file gives enum values before `=`. See [#14](https://github.com/eclipse/lsp4mp/issues/14).
 * Remove references to quarkusLanguageServer. See [#11](https://github.com/eclipse/lsp4mp/pull/11).
 * Rename settings prefix to microprofile. See [#4](https://github.com/eclipse/lsp4mp/pull/4).

### Build

 * Require Java 11 to build/run. See [#12](https://github.com/eclipse/lsp4mp/issues/12).
 * Collect all JUnit reports in Jenkins. See [#3](https://github.com/eclipse/lsp4mp/pull/3).

### Other

 * Add gitter link to readme. See [#76](https://github.com/eclipse/lsp4mp/pull/76).
 * List vscode-microprofile under clients. See [#72](https://github.com/eclipse/lsp4mp/pull/72).
 * Improve architecture documentation. See [#68](https://github.com/eclipse/lsp4mp/issues/68).
 * Update file headers to use EPL 2.0. See [#53](https://github.com/eclipse/lsp4mp/issues/53).
 * Add CONTRIBUTING.md. See [#49](https://github.com/eclipse/lsp4mp/pull/49).
 * Add diagram to the README describing MicroProfileLS and MicroProfile JDT. See [#35](https://github.com/eclipse/lsp4mp/pull/35).
 * Add diagram in README. See [#9](https://github.com/eclipse/lsp4mp/issues/9).
 * Update README. See [#6](https://github.com/eclipse/lsp4mp/pull/6).
 * Initial code contribution. See [#1](https://github.com/eclipse/lsp4mp/pull/1).

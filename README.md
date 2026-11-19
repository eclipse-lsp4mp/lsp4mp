# Eclipse LSP4MP - Language Server for MicroProfile

[![Build Status](https://ci.eclipse.org/lsp4mp/buildStatus/icon?job=lsp4mp%2Fmaster)](https://ci.eclipse.org/lsp4mp/job/lsp4mp/job/master/)
[![License](https://img.shields.io/badge/License-EPL%202.0-brightgreen.svg)](https://www.eclipse.org/legal/epl-2.0/)
[![Gitter chat for lsp4mp](https://badges.gitter.im/eclipse/microprofile-languageserver.svg)](https://gitter.im/eclipse/microprofile-languageserver)

A language server implementation based on the [Language Server Protocol](https://github.com/Microsoft/language-server-protocol) for [MicroProfile](https://microprofile.io/). This Language Server for MicroProfile (LSP4MP) provides core language support capabilities (such as code complete, diagnostics, quick fixes) to enable developers to quickly and easily develop applications using MicroProfile APIs.

This project contains:

- [MicroProfile Language Server](./microprofile.ls)
- [MicroProfile JDT LS Extensions](./microprofile.jdt)

You can build all projects at once by running the `buildAll.sh` script (`buildAll.bat` on Windows).

## Features

#### General

When you open a MicroProfile project, lsp4mp provides:

- Symbols to help you navigate to JAX-RS REST endpoint methods, searchable using the naming convention: `@/api/endpointName: GET`

#### Properties files

In `microprofile-config.properties` files, you will benefit with:

- Completion support for MicroProfile properties
- Hover support for MicroProfile properties
- Definition support for MicroProfile properties
- Format support for MicroProfile properties
- Validation and Quick Fix support for MicroProfile properties
- Outline support (flat or tree view)

#### Java files

In Java files, you will benefit with:

- Completion support for MicroProfile
- Hover support for MicroProfile
- Validation and Quick Fix support for MicroProfile
- Code Lens support for MicroProfile
- Code snippets

## Architecture

For information on the architecture/project structure see [architecture.md](./docs/architecture.md)

## Getting started

JDK 21 is required to build the language server and the `eclipse.jdt.ls` extension.

1. Clone this repository
2. Open the folder in your terminal / command line
3. Run ./buildAll.sh (OSX, Linux) or buildAll.bat (Windows)

## Maven coordinates

Here are the Maven coordinates for LSP4MP (replace the `X.Y.Z` version with the [latest release](https://repo.eclipse.org/content/repositories/lsp4mp-releases)):
```xml
<dependency>
  <groupId>org.eclipse.lsp4mp</groupId>
  <artifactId>org.eclipse.lsp4mp.ls</artifactId>
  <version>X>Y>Z</version>
  <classifier>uber</classifier>
  <exclusions>
    <exclusion>
      <groupId>org.eclipse.lsp4j</groupId>
      <artifactId>org.eclipse.lsp4j</artifactId>
    </exclusion>
    <exclusion>
      <groupId>org.eclipse.lsp4j</groupId>
      <artifactId>org.eclipse.lsp4j.jsonrpc</artifactId>
    </exclusion>
  </exclusions>
</dependency>
```

for Gradle:
```
compile(group: 'org.eclipse.lsp4mp', name: 'org.eclipse.lsp4mp', version: 'X.Y.Z', classifier: 'uber')
```

You will have to reference the Maven repository hosting the dependency you need. E.g. for Maven, add this repository to your pom.xml or settings.xml :
```xml
<repository>
  <id>lsp4mp-releases</id>
  <url>https://repo.eclipse.org/content/repositories/lsp4mp-releases/</url>
  <snapshots>
    <enabled>false</enabled>
  </snapshots>
  <releases>
    <enabled>true</enabled>
  </releases>
</repository>
```

And if you want to consume the SNAPSHOT builds instead:
```xml
<repository>
  <id>lsp4mp-snapshots</id>
  <url>https://repo.eclipse.org/content/repositories/lsp4mp-snapshots/</url>
  <releases>
    <enabled>false</enabled>
  </releases>
  <snapshots>
    <enabled>true</enabled>
  </snapshots>
</repository>
```

## Clients

- Visual Studio Code with [vscode-microprofile](https://github.com/redhat-developer/vscode-microprofile)
  - vscode-microprofile can optionally be [extended](#extensions) with [vscode-quarkus](https://github.com/redhat-developer/vscode-quarkus)
- IntelliJ with [intellij-quarkus](https://github.com/redhat-developer/intellij-quarkus)
- Eclipse IDE with [jbosstools-quarkus](https://github.com/jbosstools/jbosstools-quarkus)

## Extensions

Both the [MicroProfile JDT LS Extensions](./microprofile.jdt) and [MicroProfile Language Server](./microprofile.ls) can be extended to provide additional functionality. A common extension is to provide [additional snippets via an external JAR](https://github.com/eclipse/lsp4mp/tree/master/microprofile.ls#adding-new-external-snippets).

Example extensions:

- [Quarkus JDT LS Extension](https://github.com/redhat-developer/quarkus-ls/tree/master/quarkus.jdt.ext)
- [Quarkus LS Extension](https://github.com/redhat-developer/quarkus-ls/tree/master/quarkus.ls.ext)

## Feedback

Please report bugs, issues and feature requests by creating a [GitHub Issue](https://github.com/eclipse/lsp4mp/issues)

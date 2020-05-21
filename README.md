# Kotlin autoformatter

This is an automated and opinionated tool for reformatting Kotlin source files. Source files are
reformatted so that the code fits within a specified column limit (by default, 100 columns, as
specified by the Google Kotlin style guide) and line breaks occur at reasonable points.

The tool can be run as a library, a standalone program, or a simple Gradle plugin.

This tool is still work in progress, but can already handle a wide variety of cases.

## Upcoming work

 * Completion of the documentation.
 * Ensuring that the output does not conflict with the Kotlin coding conventions or the Google
   Kotlin style guide.
 * Organizing of imports and removal of unused imports.
 * Enforcing more horizontal whitespace rules from the Kotlin coding conventions.
 * Autoformatting KDoc.
 * Improving the autoformatting of multiline string templates which use `trimIndent()`.
 * Creation of an IntelliJ plugin.
 * Configuration of the maximum line length from `.editorconfig`.

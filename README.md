# SnippetIDE

[![Build status](https://ci.appveyor.com/api/projects/status/9ptq41x6567m9hjj?svg=true)](https://ci.appveyor.com/project/rrev/snippetide)

Just an application to compile and run snippets. Uses Java 8. Screenshot beta layout, [here](http://i.imgur.com/pka5IYI.png)

It's still alpha and not complete.

- **SnippetIDE-IDE**: is the source code of the IDE itself. 
- **SnippetIDE-API**: are the IDE api. Used to create plugins for the IDE.
- **JavaLang**: Plugin which adds the ability to compile and run the Java snippets
- **PlainText**: Plugin which allows the user to create .txt files
- **Javascript**: Plugin which adds support for Javascript
- **ShareServices**: Plugin which adds GitHub Gist / Pastebin share

The IDE loads the plugins during the boot phase, from the IDEDir/plugins directory. Use --applicationdir= to change the directory

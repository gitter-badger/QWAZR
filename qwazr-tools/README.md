QWAZR Tools
===========

The tools are a set of objects available in the application.

These objects are defined in a configuration file named **tools.json**
located in the root of your application directory.

Each tool object is defined by its Java class and a set of customized properties.

Here is an example of **tools.json** configuration file:


```json
{
  "tools": [
    {
      "name": "freemarker",
      "class": "com.qwazr.tools.FreeMarkerTool"
    },
    {
      "name": "markdown",
      "class": "com.qwazr.tools.MarkdownTool",
      "extensions": [
        "hardwraps",
        "autolinks",
        "fenced_code_blocks",
        "atxheaderspace"
      ]
    },
    {
      "name": "properties",
      "class": "com.qwazr.tools.PropertiesTool",
      "path": "site.properties"
    }
  ]
}
```

In your Web application, these objects are exposed in your **Javascript controllers** scripts by the global variable **tools**. Therefore you can use them like this:

```js
//Use the Markdown tool to convert my Markdown file as HTML
var html = tools.markdown.toHtml('my_files/README.md')

// Put the HTML as an attribute of the request object
request.attributes.currentfile = html

// Use the Freemarker tool to display the template
tools.freemarker.template("templates/my_template.ftl", request, response)
```
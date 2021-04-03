# Simple CGI Hello World example (linux example)
The following bash script can sort-of be run as a CGI script, it shows Hello World in a header.

```bash
#!/bin/bash

# Status code
echo "Status: 200 OK"

# Content header
echo "Content-type: text/html"

# Other headers (such as example cookies)
echo "Set-Cookie: test1=test"
echo "Set-Cookie: test2=hi"

# Post header, stdin is the post body, though it needs a content provider to work
# echo "Test: $(read test; echo $test)"

echo

# CGI Code example:
# The following sets PATH_INFO to 'none' if the request url does not have anything
# in the path after the script file
#
if [ "$PATH_INFO" == "" ]; then
    PATH_INFO="none"
fi


# End of headers, start of HTML, it would display: Hello World, requested sub-path: <something>
echo "<html>"
echo "    <h1>Hello World, requested sub-path: $PATH_INFO</h1>"
echo "</html>"
```

Comment-free script:

```bash
#!/bin/bash

echo "Status: 200 OK"
echo "Content-type: text/html"
echo "Set-Cookie: test1=test"
echo "Set-Cookie: test2=hi"
echo

if [ "$PATH_INFO" == "" ]; then
    PATH_INFO="none"
fi

echo "<html>"
echo "    <h1>Hello World, requested sub-path: $PATH_INFO</h1>"
echo "</html>"
```

Mark as executable:

```bash
chmod +x <yourscriptfile>
```

# Simple CGI file processor (linux example)
The following script replaces all occurrences of !HI! with Hello in the requested script.

```bash
#!/bin/bash

# Status code
echo "Status: 200 OK"

# Content header
echo "Content-type: text/html"

# End of headers
echo


# Read the input script
file_data="$(cat "$SCRIPT_FILENAME")"

# Replacec all occurrences of !HI! with Hello
file_data=${file_data//!HI!/Hello}


# Print the output
echo "$file_data"
```

Comment-free script:

```bash
#!/bin/bash

echo "Status: 200 OK"
echo "Content-type: text/html"
echo

file_data="$(cat "$SCRIPT_FILENAME")"
file_data=${file_data//!HI!/Hello}

echo "$file_data"
```

Mark as executable:

```bash
chmod +x <yourscriptfile>
```

Example file:

```html
<html>
	<h1>My !HI! World CGI Script</h1>
</html>
```

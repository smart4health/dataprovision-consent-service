# Developing with VSCode

Create a folder in the root of the project called `.vscode` and create a file within called `settings.json`. Then add the following:

```json
{
  "editor.tabSize": 2,
  "editor.insertSpaces": true,
  "editor.detectIndentation": false,
  "editor.defaultFormatter": "esbenp.prettier-vscode",
  "[javascript]": {
    "editor.defaultFormatter": "esbenp.prettier-vscode"
  },
  // Default (format when you paste)
  "editor.formatOnPaste": true,
  // Default (format when you save)
  "editor.formatOnSave": true
}
```

Make sure you have the VSCode prettier extension and it should automatically format your code.

const path = require('path');

const sourceMap = process.env.NODE_ENV === "production" ? "cheap-module-source-map" : "eval-source-map";

module.exports = {
  entry: "./src/main/javascript/index.ts",
  devtool: sourceMap,
  mode: process.env.NODE_ENV,
  output: {
    filename: 'signing-bundle.js',
    path: path.resolve(__dirname, 'build/dist/static'),
  },
  module: {
    rules: [
      {
        test: /\.ts?$/,
        use: 'ts-loader',
        exclude: /node_modules/,
      },
    ],
  },
  resolve: {
    extensions: [ '.tsx', '.ts', '.js' ],
  },
};

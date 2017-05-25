# bottledynamo

![Build Status](https://travis-ci.org/jpzk/bottledynamo.svg?branch=master) [![codecov](https://codecov.io/gh/jpzk/bottledynamo/branch/master/graph/badge.svg)](https://codecov.io/gh/jpzk/bottledynamo) [![License](http://img.shields.io/:license-Apache%202-grey.svg)](http://www.apache.org/licenses/LICENSE-2.0.txt) [![GitHub stars](https://img.shields.io/github/stars/jpzk/bottledynamo.svg?style=flat)](https://github.com/jpzk/bottledynamo/stargazers) 



Bottle Dynamo is a good enough DynamoDB wrapper for putting and getting case classes in Scala. It uses Twitter's Futures and Circe as JSON serialization.

* Support for exact-match get
* Support for range queries (numbers)

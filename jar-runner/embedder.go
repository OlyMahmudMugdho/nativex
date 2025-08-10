package main

import "embed"

//go:embed jre/** app.jar
var embeddedFiles embed.FS

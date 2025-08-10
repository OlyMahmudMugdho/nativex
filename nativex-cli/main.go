package main

import (
	"flag"
	"fmt"
	"io"
	"os"
	"os/exec"
	"path/filepath"
)

func main() {
	jrePath := flag.String("jre-path", "", "Path to JRE folder")
	jarPath := flag.String("jar-path", "", "Path to JAR file")
	osName := flag.String("os-name", "linux", "Target OS name (linux, windows, darwin)")
	arch := flag.String("arch", "amd64", "Target architecture (amd64, arm64)")
	buildName := flag.String("build-name", "", "Name of the build output file (e.g. app.exe)")
	buildLocation := flag.String("build-location", "./dist", "Directory where the build output file will be saved")
	flag.Parse()

	if *jrePath == "" || *jarPath == "" {
		fmt.Println("Usage: jar-builder --jre-path /path/to/jre --jar-path /path/to/app.jar --os-name linux --arch amd64 --build-name app --build-location /path/to/output")
		os.Exit(1)
	}

	// Determine build output name
	finalBuildName := *buildName
	if finalBuildName == "" {
		finalBuildName = "jar-runner"
		if *osName == "windows" {
			finalBuildName += ".exe"
		}
	} else {
		if *osName == "windows" && filepath.Ext(finalBuildName) != ".exe" {
			finalBuildName += ".exe"
		}
	}

	fmt.Printf("JRE Path: %s\nJAR Path: %s\nOS Name: %s\nArch: %s\nBuild Name: %s\nBuild Location: %s\n",
		*jrePath, *jarPath, *osName, *arch, finalBuildName, *buildLocation)

	jarRunnerDir := "./jar-runner"
	jreDst := filepath.Join(jarRunnerDir, "jre")
	jarDst := filepath.Join(jarRunnerDir, "app.jar")
	embedderFile := filepath.Join(jarRunnerDir, "embedder.go")

	// Copy jre and jar before build
	os.RemoveAll(jreDst)
	err := copyDir(*jrePath, jreDst)
	checkErr(err)

	err = copyFile(*jarPath, jarDst)
	checkErr(err)

	// Generate embedder.go
	embedderCode := `package main

import "embed"

//go:embed jre/** app.jar
var embeddedFiles embed.FS
`
	err = os.WriteFile(embedderFile, []byte(embedderCode), 0644)
	checkErr(err)
	fmt.Println("Generated jar-runner/embedder.go")

	absBuildLocation, err := filepath.Abs(*buildLocation)
	checkErr(err)
	os.MkdirAll(absBuildLocation, 0755)
	outputPath := filepath.Join(absBuildLocation, finalBuildName)

	// Build the binary
	cmd := exec.Command("go", "build", "-o", outputPath, ".")
	cmd.Dir = jarRunnerDir
	cmd.Env = append(os.Environ(),
		"GOOS="+*osName,
		"GOARCH="+*arch,
	)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr

	fmt.Printf("Building jar-runner for %s/%s...\n", *osName, *arch)
	err = cmd.Run()

	// Cleanup copied files no matter what
	cleanupErr := os.RemoveAll(jreDst)
	if cleanupErr != nil {
		fmt.Println("Warning: failed to remove jre folder:", cleanupErr)
	}
	cleanupErr = os.Remove(jarDst)
	if cleanupErr != nil {
		fmt.Println("Warning: failed to remove app.jar file:", cleanupErr)
	}

	checkErr(err) // Check build error after cleanup

	fmt.Println("Build succeeded! Binary output:", outputPath)
}

// utility funcs
func checkErr(err error) {
	if err != nil {
		fmt.Fprintln(os.Stderr, "Error:", err)
		os.Exit(1)
	}
}

func copyFile(src, dst string) error {
	in, err := os.Open(src)
	if err != nil {
		return err
	}
	defer in.Close()

	out, err := os.Create(dst)
	if err != nil {
		return err
	}
	defer out.Close()

	_, err = io.Copy(out, in)
	if err != nil {
		return err
	}
	return out.Sync()
}

func copyDir(src, dst string) error {
	return filepath.Walk(src, func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}

		rel, err := filepath.Rel(src, path)
		if err != nil {
			return err
		}
		targetPath := filepath.Join(dst, rel)

		if info.IsDir() {
			return os.MkdirAll(targetPath, info.Mode())
		}

		return copyFile(path, targetPath)
	})
}

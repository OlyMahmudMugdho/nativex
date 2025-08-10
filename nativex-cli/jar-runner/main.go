package main

import (
	"fmt"
	"io/fs"
	"log"
	"os"
	"os/exec"
	"path/filepath"
)

// Embed JRE and the JAR file


func main() {
	// Create a temp dir for extraction
	tmpDir, err := os.MkdirTemp("", "go_jre_run")
	if err != nil {
		log.Fatal("Failed to create temp dir:", err)
	}
	defer os.RemoveAll(tmpDir) // Clean up when done

	// Extract embedded files to tempDir
	err = fs.WalkDir(embeddedFiles, ".", func(path string, d fs.DirEntry, err error) error {
		if err != nil {
			return err
		}
		if d.IsDir() {
			return nil
		}

		data, err := embeddedFiles.ReadFile(path)
		if err != nil {
			return err
		}

		destPath := filepath.Join(tmpDir, path)
		if err := os.MkdirAll(filepath.Dir(destPath), 0755); err != nil {
			return err
		}

		perm := os.FileMode(0644)
		if filepath.Base(destPath) == "java" {
			perm = 0755 // make java executable
		}
		if err := os.WriteFile(destPath, data, perm); err != nil {
			return err
		}

		return nil
	})
	if err != nil {
		log.Fatal("Failed to extract embedded files:", err)
	}

	// Run the JAR using embedded JRE
	javaPath := filepath.Join(tmpDir, "jre", "bin", "java")
	jarPath := filepath.Join(tmpDir, "app.jar")

	fmt.Println("Running:", javaPath, "-jar", jarPath)
	cmd := exec.Command(javaPath, "-jar", jarPath)
	cmd.Stdout = os.Stdout
	cmd.Stderr = os.Stderr
	err = cmd.Run()
	if err != nil {
		log.Fatal("Failed to run jar:", err)
	}
}

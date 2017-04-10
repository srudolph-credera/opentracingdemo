package main

import (
	"bytes"
	"database/sql"
	"fmt"
	"image/png"
	"log"
	"os"

	_ "github.com/mattn/go-sqlite3"
)

func main() {
	// Create new database
	os.Remove("../activity.db")

	db, err := sql.Open("sqlite3", "../activity.db")
	if err != nil {
		log.Fatal(err)
	}

	defer db.Close()

	createTable := `
	CREATE TABLE activity (
		id INTEGER NOT NULL PRIMARY KEY,
		x INTEGER NOT NULL,
		y INTEGER NOT NULL,
		level REAL NOT NULL);
	`
	if _, err = db.Exec(createTable); err != nil {
		log.Fatalf("%q: %s\n", err, createTable)
	}

	// Read image
	f, err := os.Open("./activity.png")
	if err != nil {
		log.Fatalf("Error opening image: %q\n", err)
	}

	defer f.Close()
	img, err := png.Decode(f)
	if err != nil {
		log.Fatalf("Error reading image: %q\n", err)
	}

	// Write image data to database
	for x := img.Bounds().Min.X; x < img.Bounds().Max.X; x++ {
		var buf bytes.Buffer
		buf.WriteString("INSERT INTO activity(x, y, level) VALUES")
		for y := img.Bounds().Min.Y; y < img.Bounds().Max.Y; y++ {
			if y != img.Bounds().Min.Y {
				buf.WriteRune(',')
			}

			r, g, b, _ := img.At(x, y).RGBA()
			level := float64(r+g+b) / 196606.0
			fmt.Fprintf(&buf, "(%d, %d, %#v)", x, y, level)
		}

		if _, err = db.Exec(buf.String()); err != nil {
			log.Fatalf("Error while inserting: %q\n", err)
		}
	}
}

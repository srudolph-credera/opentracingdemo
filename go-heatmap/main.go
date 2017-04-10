package main

import (
	"context"
	"database/sql"
	"fmt"
	"net/http"
	"os"
	"strconv"

	"github.com/ExpansiveWorlds/instrumentedsql"
	iot "github.com/ExpansiveWorlds/instrumentedsql/opentracing"
	sqlite3 "github.com/mattn/go-sqlite3"
	"github.com/opentracing/opentracing-go"
	"github.com/opentracing/opentracing-go/ext"
	zipkin "github.com/openzipkin/zipkin-go-opentracing"
)

var zipkinEndpoint = "http://localhost:9411/api/v1/spans"
var serviceAddress = "localhost:8081"
var serviceName = "Go Heat Map"
var db *sql.DB
var queryActivity *sql.Stmt

func handleHeatMapRequest(w http.ResponseWriter, req *http.Request) {
	wireContext, err := opentracing.GlobalTracer().Extract(
		opentracing.HTTPHeaders,
		opentracing.HTTPHeadersCarrier(req.Header))
	if err != nil {
		fmt.Printf("Could not extract span from HTTP header: %v\n", err)
	}

	err = req.ParseForm()
	if err != nil {
		fmt.Printf("Could not parse HTTP request form: %v\n", err)
	}

	span := opentracing.GlobalTracer().StartSpan(
		"Heat Map - Go",
		ext.RPCServerOption(wireContext))

	xArr, ok := req.Form["x"]
	if !ok || len(xArr) != 1 {
		fmt.Println("Could not find a single value for 'x'")
		xArr = []string{"0"}
	}

	yArr, ok := req.Form["y"]
	if !ok || len(yArr) != 1 {
		fmt.Println("Could not find a single value for 'y'")
		yArr = []string{"0"}
	}

	span = span.SetTag("x", xArr[0])
	span = span.SetTag("y", yArr[0])

	x, err := strconv.Atoi(xArr[0])
	if err != nil {
		fmt.Printf("Unable to parse x as an integer: %v\n", err)
	} else if x < 0 || x >= 1000 {
		fmt.Printf("X value out of range, setting to 0: %v\n", x)
		x = 0
	}

	y, err := strconv.Atoi(yArr[0])
	if err != nil {
		fmt.Printf("Unable to parse y as an integer: %v\n", err)
	} else if y < 0 || y >= 1000 {
		fmt.Printf("Y value out of range, setting to 0: %v\n", y)
		y = 0
	}

	var level float64
	ctx := opentracing.ContextWithSpan(req.Context(), span)
	row := queryActivity.QueryRowContext(ctx, x, y)
	err = row.Scan(&level)
	if err != nil {
		fmt.Printf("Unable to process result as float64: %v\n", err)
	}

	activity := strconv.FormatFloat(level, 'f', -1, 64)
	span = span.SetTag("result", activity)
	defer span.Finish()

	req = req.WithContext(ctx)

	w.Write([]byte(activity))
}

func main() {
	// Initialize Zipkin
	collector, err := zipkin.NewHTTPCollector(zipkinEndpoint)
	if err != nil {
		fmt.Printf("Could not connect to Zipkin @ %s: %v\n", zipkinEndpoint, err)
		os.Exit(-1)
	}

	recorder := zipkin.NewRecorder(collector, false, serviceAddress, serviceName)
	tracer, err := zipkin.NewTracer(recorder, zipkin.ClientServerSameSpan(false), zipkin.TraceID128Bit(true))
	if err != nil {
		fmt.Printf("Could not create Zipkin tracer: %v\n", err)
		os.Exit(-1)
	}

	// Initialize OpenTracing wrapper
	opentracing.SetGlobalTracer(tracer)

	// Initialize database
	sql.Register("instrumented-sqlite3", instrumentedsql.WrapDriver(&sqlite3.SQLiteDriver{}, instrumentedsql.WithTracer(iot.NewTracer())))
	db, err = sql.Open("instrumented-sqlite3", "../activity.db")
	if err != nil {
		fmt.Printf("Could not open database: %v\n", err)
		os.Exit(-1)
	}

	ctx := context.Background()
	span, spanCtx := opentracing.StartSpanFromContext(ctx, "Prepare SQL")
	queryActivity, err = db.PrepareContext(spanCtx, "SELECT level FROM activity WHERE x = $1 AND y = $2")
	if err != nil {
		fmt.Printf("Could not prepare query: %v\n", err)
		os.Exit(-1)
	}

	span.Finish()

	// Start server
	mux := http.NewServeMux()
	mux.HandleFunc("/heatmap", handleHeatMapRequest)
	fmt.Printf("Service %v started at %v\n", serviceName, serviceAddress)
	http.ListenAndServe(serviceAddress, mux)
}

package main

import (
	"fmt"
	"math/rand"
	"net/http"
	"os"
	"strconv"
	"time"

	"github.com/opentracing/opentracing-go"
	"github.com/opentracing/opentracing-go/ext"
	zipkin "github.com/openzipkin/zipkin-go-opentracing"
)

var zipkinEndpoint = "http://localhost:9411/api/v1/spans"
var serviceAddress = "localhost:8081"
var serviceName = "Go Heat Map"

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
	span = span.SetTag("x", req.Form["x"][0])
	span = span.SetTag("y", req.Form["y"][0])
	activity := strconv.FormatFloat(rand.Float64(), 'f', -1, 64)
	span = span.SetTag("result", activity)
	defer span.Finish()

	ctx := opentracing.ContextWithSpan(req.Context(), span)
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

	rand.Seed(time.Now().UnixNano())
	mux := http.NewServeMux()
	mux.HandleFunc("/heatmap", handleHeatMapRequest)
	fmt.Printf("Service %v started at %v\n", serviceName, serviceAddress)
	http.ListenAndServe(serviceAddress, mux)
}

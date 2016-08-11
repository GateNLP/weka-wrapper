/*
 *  Copyright (c) The University of Sheffield.
 *
 *  This file is free software, licensed under the 
 *  GNU Library General Public License, Version 2.1, June 1991.
 *  See the file LICENSE.txt that comes with this software.
 *
 */
package gate.lib.wekawrapper;

import fi.iki.elonen.NanoHTTPD;
import gate.lib.interaction.data.SparseDoubleVector;
import gate.lib.wekawrapper.utils.WekaWrapperUtils;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import weka.classifiers.Classifier;
import weka.core.Instances;


/**
 * Minimal HTTPD server that should apply a model to data it receives via HTTP requests.
 * 
 * This uses a thread pool by default. The number of threads, the port, the bind address
 * the model file and the header file  are command line options when starting the server.
 * 
 * The server accepts POST requests of either JSON or binary data which must be identified
 * as application/json or application/octet-stream. If it is octet-stream the binary content
 * is assumed a Java ObjectStream representation of a SparseDoubleVector instance. In the 
 * case of JSON, it is a JSON representation of the same.
 * The result is returned as either JSON or octet-stream representation of a double array
 * containing a single value if it is just the class index or target value, or several values
 * if it is a class probability distribution. 
 * 
 * NOTE: The Weka classifier instance may or may not be thread-safe, but there are no guarantees.
 * Thread-safety of the classifier may depend on the concrete instance of the classifier.
 * For now we simply synchronise the call to the classifier, which will be a bottle neck 
 * quickly. However it is not clear yet how to map copies of the classifier to each of the 
 * threads in the thread pool used by NanoHTTPD. Once this is clear we should use copies
 * of the classifier instance for each thread in the pool. If this is not possible we have
 * to create or own thread pool just for the classification and use that. 
 * (Note there is a Weka function for creating copies of classifiers!!)
 * 
 * NOTE: at least for now we use the standard threading model (without a thread pool) if 
 * the number of threads is set to 0. In this case a new thread is created for each
 * (keep-alive) http connection to a client.
 * 
 * @author Johann Petrak
 */
public class WekaApplicationServer extends NanoHTTPD {

  private static final Logger logger = Logger.getLogger(WekaApplicationServer.class.getName());
  
  public WekaApplicationServer(int port) throws IOException {
    super(port);
  }
  public WekaApplicationServer(String hostname, int port) throws IOException {
    super(hostname, port);
  }
  
  private static Instances dataset;
  private static Classifier classifier;
  
  private static boolean haveShutdown = false;
  
  
  public static void main(String[] args) {
    // Parse the options and check values: we expect the following parameters in this order
    // model file
    // arff header file
    // port number
    // number of threads: specifies the number of concurrent worker threads for the web server
    //   and for the weka classifier
    // NOTE: it seems it is not possible to specify the IP address to bind to???
    
    if(args.length != 4) {
      System.err.println("Need exactly 4 arguments: modelfile, arffheaderfile, portnumber, nthreads");
      System.exit(1);
    }
    
    System.err.println("Loading model "+args[0]);
    classifier = WekaWrapperUtils.loadClassifier(args[0]);
    System.err.println("Loading headers "+args[1]);
    dataset = WekaWrapperUtils.loadDataset(args[1]);
    
    int port = Integer.parseInt(args[2]);
    int nrThreads = Integer.parseInt(args[3]);
    
    WekaApplicationServer server = null;
    try {      
      server = new WekaApplicationServer(port);
      System.err.println("Server created on port "+port);
    } catch (IOException ex) {
      System.err.println("Could not start server");
      ex.printStackTrace(System.err);
      System.exit(1);
    }
    if(nrThreads > 0) {
      ExecutorService pool = Executors.newFixedThreadPool(nrThreads);
      BoundRunner runner = new BoundRunner(pool);
      server.setAsyncRunner(runner);
      System.err.println("Thread pool created for nr threads: "+nrThreads);
    }
    boolean error = false;
    try {
      server.start(NanoHTTPD.SOCKET_READ_TIMEOUT,false);
      System.err.println("Server started, socket timeout is "+NanoHTTPD.SOCKET_READ_TIMEOUT);
      // now wait until we get the shutdown flag;
      while(!haveShutdown) {
        try {
          TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException ex) {
          //
        }
        //System.err.println("Checking for shutdown flag");
      }
      System.err.println("Exiting");
      // this will not just exit, so we try to explicitly exit 
      // This is a hack, we need to find out how to properly shut ourselves down
      System.exit(0);
    } catch (IOException ex) {
      System.err.println("Could not start server");
      ex.printStackTrace(System.err);
      error = true;
    } finally {
      // Not sure if we need to clean up something here
    }
    if(error) System.exit(1);
    
  }
  
  @Override
  public Response serve(IHTTPSession session) {
    try {
      String uri = session.getUri();
      System.err.println("Got the URI: "+uri);
      Map<String, String> headers = session.getHeaders();
      System.err.println("Headers: "+headers);
      // if the uri is /stop we stop the servers
      if(uri.equals("/stop")) {        
        // return an empty OK response after starting a thread that should shut down 
        // the whole server 500ms later. This is a bit of a hack but should work for our 
        // purposes, because we should never get any additional requests once the stop
        // request has been received.
        new Thread(new Runnable() {
          @Override
          public void run() {
            try {
              Thread.sleep(1500);
              //System.err.println("Stopping ");
              //stop();              
              System.err.println("Signalling shutdown");
              haveShutdown = true;
            } catch (InterruptedException ex) {
              //
            }
          }
        }).start();
        System.err.println("Sending response");
        Response res  =  newFixedLengthResponse(Response.Status.OK, "text/plain", "Shutting down"); 
        res.addHeader("Connection", "close");
        return res;
      }

      Method method = session.getMethod();      
      System.err.println("Method="+method);
      if(method != Method.POST) {
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Only accepting POST requests");
      }
      // check the content type header
      String contentType = headers.get("content-type");
      System.err.println("COntent type is "+contentType);
      if(contentType == null || (!contentType.equals("application/json") && !contentType.equals("application/octet-stream"))) {
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Only accepting application/json or application/octet-stream content");        
      }
      String accept = headers.get("accept");
      System.err.println("Accept is "+contentType);
      if(accept == null || (!accept.equals("application/json") && !accept.equals("application/octet-stream"))) {
        return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Only sending application/json or application/octet-stream content");        
      }
      //Map<String, List<String>> parms = session.getParameters();
      Map<String,String> parms = session.getParms();
      System.err.println("Parms: "+parms);
      HashMap<String,String> bodyMap = new HashMap<String,String>();
      
      
      String remoteIP = session.getRemoteIpAddress();
      System.err.println("From remote IP: "+remoteIP);
      
      // Actual handling of the classification request ....
      if(contentType.equals("application/json")) {
        session.parseBody(bodyMap);
        System.err.println("BodyMap: "+bodyMap);
        String json = bodyMap.get("postData");
        if(json == null || json.trim().isEmpty()) {
          return newFixedLengthResponse(Response.Status.BAD_REQUEST, "text/plain", "Empty request body"); 
        }
        // parse json into sparse vector
        SparseDoubleVector sdv = WekaWrapperUtils.json2sdv(json);
        double ret[] = WekaWrapperUtils.classifyInstance(sdv, classifier, dataset);
        if(accept.equals("application/json")) {
          // convert ret to json and return
          String retjson = WekaWrapperUtils.pred2json(ret);
          return newFixedLengthResponse(Response.Status.OK,"application/json",retjson);
        } else {
          // convert ret to ObjectStream buffer and return
          byte[] retbin = WekaWrapperUtils.pred2binary(ret);
          ByteArrayInputStream bis = new ByteArrayInputStream(retbin);
          return newFixedLengthResponse(Response.Status.OK, "application/binary", bis, retbin.length);        
        }
      } else { // must be octet-stream
        // get binary data
        InputStream is = session.getInputStream();
        ByteArrayOutputStream bos = new ByteArrayOutputStream(2048);
        byte[] read = new byte[512];
        for(int i; -1 != (i = is.read(read)); bos.write(read,0,i));
        is.close();
        byte[] buffer = bos.toByteArray();
        // parse object stream into vector
        SparseDoubleVector sdv = WekaWrapperUtils.binary2sdv(buffer);
        double ret[] = WekaWrapperUtils.classifyInstance(sdv, classifier, dataset);
        if(accept.equals("application/json")) {
          // convert ret to json and return
          String retjson = WekaWrapperUtils.pred2json(ret);
          return newFixedLengthResponse(Response.Status.OK,"application/json",retjson);          
        } else {
          // convert ret to ObjectStream buffer and return
          byte[] retbin = WekaWrapperUtils.pred2binary(ret);
          ByteArrayInputStream bis = new ByteArrayInputStream(retbin);
          return newFixedLengthResponse(Response.Status.OK, "application/binary", bis, retbin.length);                  
        }
      }
    } catch (Exception ex) {
      Logger.getLogger(WekaApplicationServer.class.getName()).log(Level.SEVERE, null, ex);
      return newFixedLengthResponse(Response.Status.NOT_ACCEPTABLE,"text/plain",ex.getMessage());
    }
  }
  
  
  /**
   * Use a thread pool for running.
   */
  static class BoundRunner implements NanoHTTPD.AsyncRunner {
    private ExecutorService executorService;
    private final List<ClientHandler> running =
            Collections.synchronizedList(new ArrayList<ClientHandler>());

    public BoundRunner(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Override
    public void closeAll() {
        // copy of the list for concurrency
        for (ClientHandler clientHandler : new ArrayList<ClientHandler>(this.running)) {
            clientHandler.close();
        }
    }

    @Override
    public void closed(ClientHandler clientHandler) {
        this.running.remove(clientHandler);
    }

    @Override
    public void exec(ClientHandler clientHandler) {
        executorService.submit(clientHandler);
        this.running.add(clientHandler);
    }
}
  
}

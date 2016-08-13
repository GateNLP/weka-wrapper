// script to test basic functionality of the server

@Grab(group="com.mashape.unirest",module="unirest-java",version="1.4.9")
@Grab(group="com.eclipsesource.minimal-json",module="minimal-json",version="0.9.4")

import  com.eclipsesource.json.*
import  com.mashape.unirest.http.*


values = [1.0,0.0,0.99539,-0.05889,0.85243,0.02306,0.83398,-0.37708,1.0,0.0376,0.85243,
-0.17755,0.59755,-0.44945,0.60536,-0.38223,0.84356,-0.38542,0.58212,-0.32192,0.56971,
-0.29674,0.36946,-0.47357,0.56811,-0.51171,0.41078,-0.46168,0.21266,-0.3409,0.42267,
-0.54487,0.18641,-0.453] as double[]
indices = 0..(values.size()-1) as int[]
println("Trying to connect")
jv = Json.array(values)
jv = Json.array().add(jv)
ji = Json.array(indices)
ji = Json.array().add(ji)
json = Json.object().add("values",jv)
json.add("indices",ji)
println("json="+json)

resp = Unirest.post("http://localhost:7000/").
  header("accept","application/json").
  header("content-type","application/json").
  body(json.toString()).
  asString() 

status = resp.getStatus()
println("Got status"+status)

if(status == 200) {
  body = resp.getBody()
  println("Got body, class="+body.getClass()+" obj="+body)
}

Unirest.post("http://localhost:7000/stop").asString()

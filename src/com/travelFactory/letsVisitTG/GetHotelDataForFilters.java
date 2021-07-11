package com.travelFactory.letsVisitTG;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class GetHotelDataForFilters extends HttpServlet{
	
	private static HttpURLConnection connection;
	private static final long serialVersionUID = 1L;

	/**
     * @see HttpServlet#HttpServlet()
     */
    public GetHotelDataForFilters() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
		
		String queryString = request.getQueryString();
		JSONObject apiKey = null;
		
		//JSON parser object to parse read file
        JSONParser jsonParser = new JSONParser();
        String path = request.getServletContext().getRealPath("/WEB-INF/keys.json");
        System.out.println(path);
        
        try (FileReader reader = new FileReader(path))
        {
            //Read JSON file
            Object obj = jsonParser.parse(reader);
            apiKey = (JSONObject) obj;
 
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        if(apiKey == null) {
        	response.setContentType("application/json");
		    response.sendError(500, "Sorry something went wrong from our side, please refresh the page");
		    return;
        }
        
		try {
		    URL url = new URL("https://rest.reserve-online.net/availability"+"?"+request.getQueryString());
		    
		    // get data
		    connection = (HttpURLConnection)url.openConnection();
		
		    connection.setRequestMethod("GET");
		    connection.setDoOutput(true);
		    connection.setRequestProperty("Authorization", "Basic " + apiKey.get("apikey"));
		    connection.setRequestProperty("Accept", "application/json");
		    
		    
		    switch (connection.getResponseCode()) {
	            case HttpURLConnection.HTTP_OK:
	            	InputStream content = connection.getInputStream();
	            	BufferedReader in = new BufferedReader(new InputStreamReader(content));
	    		    JSONParser parse = new JSONParser(); 
	    		    apiKey = (JSONObject)parse.parse(in);
	    		    response.setContentType("application/json");
	    			response.getWriter().print(apiKey);
	                break; // fine, go on
	            case HttpURLConnection.HTTP_BAD_REQUEST:
	            	System.out.println(" **some query parameter problem**.");
	            	InputStream contentErrorBadRequest = connection.getErrorStream();
	            	BufferedReader inErrorBadRequest = new BufferedReader(new InputStreamReader(contentErrorBadRequest));
	    		    JSONParser parseErrorBadRequest = new JSONParser(); 
	    		    apiKey = (JSONObject)parseErrorBadRequest.parse(inErrorBadRequest);
	    		    response.setContentType("application/json");
	    		    response.sendError(400, apiKey.get("error_msg").toString());
	    			break;
	            default:
	            	System.out.println(" **unknown response code**.");
	            	InputStream contentError = connection.getErrorStream();
	            	BufferedReader inError = new BufferedReader(new InputStreamReader(contentError));
	    		    JSONParser parseError = new JSONParser(); 
	    		    apiKey = (JSONObject)parseError.parse(inError);
	    		    response.setContentType("application/json");
	    			response.getWriter().print(apiKey);
		    }

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (Exception e) {
		    e.printStackTrace();
		}
		finally {
			connection.disconnect();
		}
	}
}

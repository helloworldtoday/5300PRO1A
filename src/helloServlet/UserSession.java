package helloServlet; 

import java.io.*;

import javax.servlet.*;
import javax.servlet.annotation.*;
import javax.servlet.http.*;

import java.util.*;

// Display the Site Behavior
@WebServlet("/hello")
public class UserSession extends HttpServlet {
  //@Override
	
	// Restrict the length of session state
	private final int maxLength = 512;
	
	// Create session data structure <sessionID, version, message, expiration-timestamp>
	class SessionData{
		String sessionID;
	    int version; // refresh/replace version++
	    String message; // text typed
	    Calendar expirationTime;
	  
	    // Initialize session data
	    SessionData(String s, int v, String m, Calendar e){
	    	sessionID = s;
	    	version = v;
	    	message = m;
	    	expirationTime = e;
	    }  
   }
  
  HashMap<String, SessionData> sessionTable;
  
  public UserSession(){
	  super();
	  sessionTable = new HashMap<String, SessionData>();
  }
  
  // Update session ID and handle synchronized update 
  private synchronized String updateId(){
	  return UUID.randomUUID().toString();
  }
  
  // Remove timed-out sessions from the session data table
  private void removeOutOfDate(){
	  Calendar now = Calendar.getInstance();
	  for (Map.Entry<String, SessionData> entry: sessionTable.entrySet()) {
		  SessionData temp = entry.getValue();
		  if (temp.expirationTime.before(now))
			  sessionTable.remove(entry.getKey());
	  }
  }
  
  // Get the cookie value given the cookie name
  public static Cookie getCookie(HttpServletRequest request,
                                 String cookieName) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie: cookies) {
        if (cookieName.equals(cookie.getName())) {
          return(cookie);
        }
      }
    }
    return(null);
  }
  
  public void doGet(HttpServletRequest request,
                    HttpServletResponse response)
      throws ServletException, IOException {
	  
	  removeOutOfDate();
	  // Cookie[] cookies = request.getCookies();
	  SessionData newSessionData;
	  Cookie newCookie = getCookie(request, "CS5300PROJ1SESSION");
	  
	  // If the request is new, put new cookie, new session data in the table
	  if (newCookie == null) {
		  String newId = updateId();
		  Calendar newTime = Calendar.getInstance();
		  newTime.add(Calendar.SECOND, 5);
		  newSessionData = new SessionData(newId, 1, "Hello User!", newTime);
		  
		  // handle synchronized request
		  synchronized(sessionTable) {
		  	sessionTable.put(newId, newSessionData);
		  }
		  
		  // Create new cookie with expiration time 5 seconds
		  newCookie = new Cookie("CS5300PROJ1SESSION", newId + "_1_locationMetadata");
		  newCookie.setMaxAge(5);
	  }
	  // If the request is old such as refresh, update version number and expiration time
	  else {
		  // Update expiration time in cookie
		  newCookie.setMaxAge(5);
		  
		  // Get cookie content, including sessionID, version number
		  String[] cookieContent = newCookie.getValue().split("_");
		  
		  // handle synchronized request
		  synchronized(sessionTable) {
			  newSessionData = sessionTable.get(cookieContent[0]);
		  }
		  // update the content of session table
		  newSessionData.version++;
		  newSessionData.expirationTime = Calendar.getInstance();
		  newSessionData.expirationTime.add(Calendar.SECOND, 5);
		  newCookie.setValue(newSessionData.sessionID + "_"
				  			+ newSessionData.version + "_locationMetadata");
	  }
	  response.addCookie(newCookie);
	  response.setContentType("text/html");
	  PrintWriter out = response.getWriter();
	  display(out, newSessionData.message, newCookie.getValue(), newSessionData.expirationTime.getTime());
  }
  
  public void doPost(HttpServletRequest request,
          			 HttpServletResponse response)
      throws ServletException, IOException {
	  removeOutOfDate();
	  // Cookie[] cookies = request.getCookies();
	  Cookie newCookie = getCookie(request, "CS5300PROJ1SESSION");
	  response.setContentType("text/html");
	  PrintWriter out = response.getWriter();
	  
	  // if there's no cookie then create new session state
	  if(newCookie == null)
		  doGet(request, response);
	  else {
		  String[] cookieContent = newCookie.getValue().split("_");
		  SessionData newSessionData;
		  
		  // handle synchronized access to session table
		  synchronized(sessionTable) {
			  newSessionData = sessionTable.get(cookieContent[0]);	  
		  }
		  
		  newSessionData.version++;
		  newCookie.setValue(newSessionData.sessionID + "_"
				  			+ newSessionData.version + "_locationMetadata");	
		  
		  
		  // If request "replace", update site behavior corresponding to input text 
		  // and reset expiration time
		  if(request.getParameter("replace") != null) {
			  newSessionData.message = request.getParameter("replaceContent");
			  //restrict the length of input message to 512
			  if (newSessionData.message.length() > maxLength)
				  newSessionData.message.substring(0, maxLength);
			  // update expiration time
			  newCookie.setMaxAge(5);
			  newSessionData.expirationTime = Calendar.getInstance();
			  newSessionData.expirationTime.add(Calendar.SECOND, 5);
		  }
		  // If request "refresh", merely update expiration time
		  else if (request.getParameter("refresh") != null) {
			//update expiration time
			  newCookie.setMaxAge(5);
			  newSessionData.expirationTime = Calendar.getInstance();
			  newSessionData.expirationTime.add(Calendar.SECOND, 5);
		  }
		  // If request "logout", set cookie to null and remove corresponding session data from table
		  else if(request.getParameter("logout") != null) {
			  newCookie.setMaxAge(0);
			  newCookie.setValue(null);
			  
			  synchronized(sessionTable) {
				  sessionTable.remove(newSessionData.sessionID);
			  }
			  response.addCookie(newCookie);
			  out.println("<HTML>\n" +
				       "<HEAD><TITLE>" + "Logout" + "</TITLE></HEAD>\n" +
				       "<BODY BGCOLOR=\"#FDF5E6\">\n" +
				       "<H1>" + "Logout Successfully!" + "</H1>\n" + 
				       "</BODY></HTML>");
			  return;
		  }
		  response.addCookie(newCookie);
		  display(out, newSessionData.message, newCookie.getValue(), newSessionData.expirationTime.getTime());
	  }
  }
  
  // Print the site behavior out
  private void display(PrintWriter out, String message, String cookie, Date time) {
	  String title = "Information";  
	  String docType =
	    "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.0 " +
	    "Transitional//EN\">\n";
	    
	  out.println
	    (docType +
	       "<HTML>\n" +
	       "<HEAD><TITLE>" + title + "</TITLE></HEAD>\n" +
	       "<BODY BGCOLOR=\"#FDF5E6\">\n" +
	       "<H1>" + title + "</H1>\n" +
	       "<H2>" + message + "</H2>\n" +
	       "<form action=\"hello\" method=\"post\">\n" + 
	       "<BR> <INPUT TYPE=\"SUBMIT\" NAME = \"replace\" VALUE=\"replace\">\n" + 
	       "<INPUT TYPE=\"TEXT\" NAME=\"replaceContent\"><P>\n" + 
	       "<BR> <INPUT TYPE=\"SUBMIT\" NAME = \"refresh\" VALUE=\"refresh\">\n" + 
	       "<BR><BR> <INPUT TYPE=\"SUBMIT\" NAME = \"logout\" VALUE=\"logout\">\n" + 
	       "</form>" + 
	       "<BR><BR>" + "Cookie: " + cookie + "\n" + 
	       "<BR><BR>" + "Expiration Time: " + time + "\n" + 
	       "</BODY></HTML>");
  }
}
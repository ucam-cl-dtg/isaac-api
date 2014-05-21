package uk.ac.cam.cl.dtg.isaac.app;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.cam.cl.dtg.isaac.models.ContentInfo;
import uk.ac.cam.cl.dtg.isaac.models.ContentPage;
import uk.ac.cam.cl.dtg.segue.api.SegueApiFacade;
import uk.ac.cam.cl.dtg.segue.api.SegueGuiceConfigurationModule;
import uk.ac.cam.cl.dtg.segue.dto.Content;
import uk.ac.cam.cl.dtg.segue.dto.Figure;
import uk.ac.cam.cl.dtg.util.Mailer;
import uk.ac.cam.cl.dtg.util.PropertiesLoader;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * Rutherford Controller
 * 
 * This class specifically caters for the Rutherford physics server and is expected to provide extended functionality to the Segue api for use only on the Rutherford site.
 * 
 */
@Path("api/")
public class IsaacController {
	private static final Logger log = LoggerFactory.getLogger(IsaacController.class);
	
	private static SegueApiFacade api;

	public IsaacController(){
		// Get an instance of the segue api so that we can service requests directly from it 
		// without using the rest endpoints.
		if(null == api){
			Injector injector = Guice.createInjector(new IsaacGuiceConfigurationModule(), new SegueGuiceConfigurationModule());
			api = injector.getInstance(SegueApiFacade.class);
		}
		
//		test of user registration - this is just a snippet for future reference as I didn't know where else to put it.
//		User user = api.getCurrentUser(req);
//		// example of requiring user to be logged in.
//		if(null == user)
//			return api.authenticationInitialisation(req, "google");
//		else
//			log.info("User Logged in: " + user.getEmail());
	}
	
	@GET
	@Path("concepts/{concept}")
	@Produces("application/json")
	public ContentPage getConcept(@Context HttpServletRequest req,
			@PathParam("concept") String concept) {
		return getContentPage(req, concept);
	}

	@GET
	@Path("questions/{question}")
	@Produces("application/json")
	public ContentPage getQuestion(@Context HttpServletRequest req,
			@PathParam("question") String question) {
		
		return getContentPage(req, question);
	}
	
	@GET
	@Path("pages/{page}")
	@Produces("application/json")
	public ContentPage getContentPage(@Context HttpServletRequest req,
			@PathParam("page") String page) {

		Content c = (Content) api.getContentById(page).getEntity();
		Injector injector = Guice.createInjector(new IsaacGuiceConfigurationModule());
		PropertiesLoader propertiesLoader = injector.getInstance(PropertiesLoader.class);
		
		String proxyPath = propertiesLoader.getProperty(Constants.PROXY_PATH);
		ContentPage cp = new ContentPage(c.getId(),c,this.buildMetaContentmap(proxyPath, c));		
		return cp;
	}
	
	@GET
	@Path("images/{imageId}")
	@Produces("*/*")
	public Response getImages(@Context HttpServletRequest req,
			@PathParam("imageId") String imageId) {
		log.info("test");

		return api.getFileContent(api.getLiveVersion().getEntity().toString(), ((Figure)api.getContentById(imageId).getEntity()).getSrc());
	}
	
	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Path("search/full-site/")
	@Produces("application/json")
	public List<ContentInfo> search(@Context HttpServletRequest req, @FormParam("searchString") String searchString) {
		Injector injector = Guice.createInjector(new IsaacGuiceConfigurationModule());
		PropertiesLoader propertiesLoader = injector.getInstance(PropertiesLoader.class);
		
		String proxyPath = propertiesLoader.getProperty(Constants.PROXY_PATH);

		Response searchResponse = api.search(searchString);
		List<ContentInfo> summaryOfSearchResults = null;
		if(searchResponse.getEntity() instanceof List<?>){
			summaryOfSearchResults = this.extractContentInfo((List<Content>) searchResponse.getEntity(), proxyPath);
		}
		
		return summaryOfSearchResults;
	}	
	
	@POST
	@Consumes({"application/x-www-form-urlencoded"})
	@Path("contact-us/sendContactUsMessage")
	public ImmutableMap<String,String> postContactUsMessage(
			@FormParam("full-name") String fullName,
			@FormParam("email") String email,
			@FormParam("subject") String subject,
			@FormParam("message-text") String messageText,
			@Context HttpServletRequest request){

		Injector injector = Guice.createInjector(new SegueGuiceConfigurationModule());
		PropertiesLoader propertiesLoader = injector.getInstance(PropertiesLoader.class);
		
		// construct a new instance of the mailer object
		Mailer contactUsMailer = new Mailer(propertiesLoader.getProperty(Constants.MAILER_SMTP_SERVER), propertiesLoader.getProperty(Constants.MAIL_FROM_ADDRESS));
		
		if (StringUtils.isBlank(fullName) && StringUtils.isBlank(email) && StringUtils.isBlank(subject) && StringUtils.isBlank(messageText)){
			log.debug("Contact us required field validation error ");
			return ImmutableMap.of("result", "message not sent - Missing required field - Validation Error");			
		}
		
		// Get IpAddress of client
		String ipAddress = request.getHeader("X-FORWARDED-FOR");
		
		if (ipAddress == null) {
			ipAddress = request.getRemoteAddr();
		}

		// Construct message
		StringBuilder message = new StringBuilder();
		message.append("- Sender Details - " + "\n");
		message.append("From: " + fullName + "\n");
		message.append("E-mail: " + email + "\n");
		message.append("IP address: " + ipAddress + "\n");
		message.append("Message Subject: " + subject + "\n");
		message.append("- Message - " + "\n");
		message.append(messageText);
		
		try {
			// attempt to send the message via the smtp server
			contactUsMailer.sendMail(propertiesLoader.getProperty(Constants.MAIL_RECEIVERS).split(","), email, subject, message.toString());
			log.info("Contact Us - E-mail sent to " + propertiesLoader.getProperty(Constants.MAIL_RECEIVERS) + " " + email + " " + subject + " " + message.toString());
			
		} catch (AddressException e) {				
			log.warn("E-mail Address validation error " + e.toString());
			return ImmutableMap.of(
					"result", "message not sent - E-mail address malformed - Validation Error \n " + e.toString());		
			
		} catch (MessagingException e) {
			log.error("Messaging error " + e.toString());
			return ImmutableMap.of(
					"result", "message not sent - Unknown Messaging error\n " + e.toString());	
		}
		
		return ImmutableMap.of("result", "success");
	}	

	
	/**
	 * This method will look at a content objects related content list and return a list of contentInfo objects which can be used for creating links etc.
	 * 
	 * This method returns null if the content object provided has no related Content
	 * 
	 * @param proxyPath - the string prefix for the server being used
	 * @param content - the content object which contains related content
	 * @return
	 */
	private List<ContentInfo> buildMetaContentmap(String proxyPath, Content content){
		if(null == content){
			return null;
		}else if(content.getRelatedContent() == null || content.getRelatedContent().isEmpty()){
			return null;
		}
		
		List<ContentInfo> contentInfoList = new ArrayList<ContentInfo>();
		
		for(String id : content.getRelatedContent()){
			try{
				Content relatedContent = (Content) api.getContentById(id).getEntity();
				
				if(relatedContent == null){
					log.warn("Related content (" + id + ") does not exist in the data store.");
				} else {
					ContentInfo contentInfo = extractContentInfo(relatedContent, proxyPath);
					contentInfoList.add(contentInfo);
				}
			}
			catch(ClassCastException exception){
				exception.printStackTrace();
			}
		}
		
		return contentInfoList;
	}

	/**
	 * This method will extract basic information from a content object so the lighter ContentInfo object can be sent to the client instead.
	 * 
	 * TODO: we should use an automapper to do this in a nicer way.
	 * @param content
	 * @param proxyPath
	 * @return
	 */
	private ContentInfo extractContentInfo(Content content, String proxyPath){
		if (null == content)
			return null;
		
		// TODO fix this stuff to be less horrid
		ContentInfo contentInfo = null;
		try{
			if(content.getType().equals("image")){
				contentInfo = new ContentInfo(content.getId(), content.getTitle(), content.getType(), proxyPath + "/isaac/api/images/" + URLEncoder.encode(content.getId(), "UTF-8"));
			}
			else if(content.getType().toLowerCase().contains("question")){
				contentInfo = new ContentInfo(content.getId(), content.getTitle(), content.getType(), proxyPath + '/' + "questions/" + URLEncoder.encode(content.getId(), "UTF-8"));
			}
			else{
				contentInfo = new ContentInfo(content.getId(), content.getTitle(), content.getType(), proxyPath + '/' + content.getType().toLowerCase() + "s/" + URLEncoder.encode(content.getId(), "UTF-8"));
			}			
		}
		catch(UnsupportedEncodingException e){
			log.error("Unable to encode URL.");
			e.printStackTrace();
		}
		return contentInfo;
	}

	/**
	 * Utility method to convert a list of content objects into a list of ContentInfo Objects 
	 * @param contentList
	 * @param proxyPath
	 * @return
	 */
	private List<ContentInfo> extractContentInfo(List<Content> contentList, String proxyPath){
		if (null == contentList)
			return null;
		
		List<ContentInfo> listOfContentInfo = new ArrayList<ContentInfo>();
		
		for(Content content : contentList){
			ContentInfo contentInfo = extractContentInfo(content,proxyPath); 
			if(null != contentInfo)
				listOfContentInfo.add(contentInfo);
		}		
		return listOfContentInfo;
	}
}

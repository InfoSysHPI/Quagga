package jangada;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import org.apache.log4j.Logger;

import edu.cmu.minorthird.classify.BinaryClassifier;
import edu.cmu.minorthird.classify.MutableInstance;
import edu.cmu.minorthird.text.CharAnnotation;
import edu.cmu.minorthird.util.IOUtil;
import edu.cmu.minorthird.util.LineProcessingUtil;


/**
 * Annotator for Extracting the reply lines from email messages. It follows the description in "Learning to Extract Signature and Reply 
 * Lines from Email", V.R.Carvalho and W.W.Cohen, CEAS (Conference of Email and Anti-Span), 2004
 * 
 * @author Vitor R. Carvalho
 * May 2004
 */
public class ReplyToAnnotator extends LocalStringAnnotator
{

  private BinaryClassifier model; 
  private String modelname = "models/VPreplyModel";
  private static final int Th = 0;
  private static Logger log = Logger.getLogger(ReplyToAnnotator.class);
   // serialization stuff
  static private final long serialVersionUID = 1;
  private final int CURRENT_VERSION_NUMBER = 1;
	
 //--------------------- Constructors -----------------------------------------------------
 	 
	
  public ReplyToAnnotator(){
  	try {
		InputStream input = this.getClass().getResourceAsStream(modelname);
		model = (BinaryClassifier) IOUtil.loadSerialized(input);
		System.out.println(model.toString());
	} catch (IOException e) {
		e.printStackTrace();
		System.out.println("COULD NOT FIND MODEL FILE "+modelname);
	}
  }
 	
	
//--------------------- Methods -----------------------------------------------------

  //	
  public CharAnnotation[] annotateString(String spanString) 
  {    	  	
    ArrayList list = this.Predict(spanString);    	
    if(list.isEmpty()){return null;} 
    CharAnnotation[] cann = (CharAnnotation[])list.toArray(new CharAnnotation[list.size()]);
	return cann;
  }
  
  public String deleteReplyLinesFromMsg(String doc){
  	SigFilePredictor.WindowRepresentation windowRep = new SigFilePredictor.WindowRepresentation(doc);
  	StringBuffer notreplybuff = new StringBuffer();
  	ClassifyInstances(windowRep, "reply", notreplybuff, null);
  	return notreplybuff.toString();
  }
  
  public String getMsgReplyLines(String doc){
  	SigFilePredictor.WindowRepresentation windowRep = new SigFilePredictor.WindowRepresentation(doc);
  	StringBuffer replybuff = new StringBuffer();
  	ClassifyInstances(windowRep, "reply", null, replybuff);
  	return replybuff.toString();
  }
  
  
  public String explainAnnotation(edu.cmu.minorthird.text.TextLabels labels, edu.cmu.minorthird.text.Span documentSpan)
  {
    return "reply-to extraction - not implemented yet!";
  }
  
 /*
 *  Classifies the lines of the incoming message, as being or not a 
 *  reply line
 *
 *  @param email message in string representation
 *  @return an ArrayList with all reply lines in a CharAnnotation[] format 
 */
  private ArrayList Predict(String msg)
  {  
  	SigFilePredictor.WindowRepresentation windowRep = new SigFilePredictor.WindowRepresentation(msg);
   	ArrayList herelist = ClassifyInstances(windowRep, "reply", null, null);
   	return herelist;    	
  }
  
 /*
 *  Classifies the lines in reply or non-reply lines
 *  - It calls the serializable classifier to each msg line
 *  @param windowRepresentation of the message (inner class of SigFilePredictor)
 *  @param tag to be used ("reply")
 *  @return an ArrayList with all reply lines in a CharAnnotation[] format
 * 
 */
  private ArrayList ClassifyInstances(SigFilePredictor.WindowRepresentation windowRep, String tag, StringBuffer notreply, StringBuffer reply)
  {  	
	ArrayList bemlocal = new ArrayList();
	MutableInstance[] ins = windowRep.getInstances();
	int[] firstCharIndex = windowRep.getFirstCharIndex();
	String[] arrayOfLines = windowRep.getArrayOfLines();
	String wholeMessage = windowRep.getWholeMessage();
	int start = 0;
	for(int i=0; i<ins.length;i++){
		
		//new
		start = start + arrayOfLines[i].length();
			
		boolean decision = (model.score(ins[i])<Th)? false:true;
	    int charBegin;
		if(decision){
			//System.out.println("POSITIVE = " +i);//to debug
			//System.out.println(ins[i].toString());
			if(!(reply==null)) reply.append(arrayOfLines[i]+"\n");
//			log.debug(arrayOfLines[i]);
//			charBegin = wholeMessage.indexOf(arrayOfLines[i], firstCharIndex[i]-1);
//			if(charBegin<0) charBegin = firstCharIndex[i]; //just in case
//			//bemlocal.add(new CharAnnotation(charBegin,arrayOfLines[i].length(), tag));
//			bemlocal.add(new CharAnnotation(charBegin,arrayOfLines[i].length()+1, tag));
						
			//new
			int tee = start - arrayOfLines[i].length()+i;
			charBegin = wholeMessage.indexOf(arrayOfLines[i], tee);
			bemlocal.add(new CharAnnotation(charBegin,arrayOfLines[i].length(), tag));			
		}	
		else{
			if(!(notreply==null))  notreply.append(arrayOfLines[i]+"\n");
		}	
		
//		
//		boolean decision = (model.score(instanceArray[i])<Th)? false:true;
//		
//		//new
//		if((decision)){
//			if((arrayOfLines.length - i > 10)){//magic number - to avoid catastrofic mistakes in long messages
//				int tee = start - arrayOfLines[i].length()+i;
//				charBegin = wholeMessage.indexOf(arrayOfLines[i], tee);
//				bemlocal.add(new CharAnnotation(charBegin,arrayOfLines[i].length(), tag));
//			}
//			else{//all sigs are supposed to be in the last lines of the message
//				int tteett = start - arrayOfLines[i].length()+i;
//				charBegin = wholeMessage.indexOf(arrayOfLines[i], tteett);
//				int ultimo = wholeMessage.length();
//				bemlocal.add(new CharAnnotation(charBegin,ultimo, tag));
//				//bemlocal.add(new CharAnnotation(charBegin,arrayOfLines[i].length(), tag));
//				break;
//			}
//		}
		
		
	}
	log.debug("\n\n");
	return bemlocal;  	
  }
  
 
 
//--------------------- main method/testing -----------------------------------------------------
  
  //for testing purposes
  public static void main(String[] args)
  {
    try {    	
       //Usage check
       if (args.length < 1)
       {
         usage();
         return;
       }	
       
		//parsing inputs
       	boolean create = false;
       	String opt = args[0];
		if ((opt.startsWith("-create"))||(opt.startsWith("create"))) {
			create = true;
		}
      
       if(create){ //creates a model
       	  SigFilePredictor.createModel(args, "reply");       	
       }       
       else{ //prediction mode
       //   System.out.println("For details, set the verbosity level in config/log4j.properties\n");
          ReplyToAnnotator repto = new ReplyToAnnotator();

          for(int i=0; i< args.length; i++){
         	 String message = LineProcessingUtil.readFile(args[i]);
     	 	 CharAnnotation[] onelist = repto.annotateString(message);
 		     String onelist3 = repto.getMsgReplyLines(message);
     	 	 System.out.println("\n######### Reply Lines of "+args[i]+" #######");
 		     System.out.print(onelist3.toString());
     	 	 //System.out.println("\n######### Msg After Removing the Reply Lines  #######");
     	 	 //String onelist2 = repto.deleteReplyLinesFromMsg(message);
 		     //System.out.print(onelist2.toString()+"\n\n"); 
          }
       }    	
    } catch (Exception e) {
         usage();
         e.printStackTrace();
    }
  } 
  
  private static void usage()
  {
  	 System.out.println("usage: ReplyToAnnotator filename1 filename2 ...");
     System.out.println("OR");
     System.out.println("usage: ReplyToAnnotator -create filename1 filename2 ...");
     System.out.println("to create, use \"Signature and Reply Dataset\" annotation stile as in www.cs.cmu.edu/~vitor/codeAndData.html");
  }  
}
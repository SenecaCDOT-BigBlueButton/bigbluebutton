package org.bigbluebutton.modules.polling.model
{
	import com.asfusion.mate.events.InternalResponseEvent;
	
	import mx.collections.ArrayCollection;
	
	import org.bigbluebutton.common.LogUtil;
	import org.bigbluebutton.util.i18n.ResourceUtil;
	import org.bigbluebutton.modules.polling.model.PollStatLineObject;
	
	/*
	 *  This class has been setted his attributes to public, for serialize with the model of the bigbluebutton-apps, in order
	 *  to enable the communication. This class is used for send public and private.
	 **/
	[Bindable]
	[RemoteClass(alias="org.bigbluebutton.conference.service.poll.Poll")]
	public class PollObject
	{
		public static const LOGNAME:String = "[PollingObject] ";
		
		/* 
		 ########################################################################################
		 # KEY PLACES TO UPDATE, WHEN ADDING NEW FIELDS TO THE HASH:							#
		 # PollingService.as, buildServerPoll()													#
		 # PollingService.as, extractPoll()														#
		 # PollingInstructionsWindow.mxml, buildPoll()											#
		 # - Only necessary when the new field is involved with poll creation					#
		 # Don't forget to update the server side as well (key locations found in Poll.java)	#
		 ########################################################################################
		*/
		
		public var title:String;
		public var room:String;
		public var isMultiple:Boolean;
		public var question:String;
		public var answers:Array;
		public var votes:Array;
		public var time:String;
		public var totalVotes:int;
		public var status:Boolean;
		public var didNotVote:int;
		public var publishToWeb:Boolean;
		public var webKey:String = new String;
		
		public var answerT:String = "TEST1";
		public var votesT:String = "TEST2";
		public var percentT:String = "TEST3";
		
		// For developer use, this method outputs all fields of a poll into the debug log for examination.
		// Please remember to add lines for any new fields that may be added.
		public function checkObject():void{
			if (this != null){
				LogUtil.debug(LOGNAME + "Running CheckObject on the poll with title " + title);
				LogUtil.debug(LOGNAME + "Room is: " + room);
				LogUtil.debug(LOGNAME + "isMultiple is: " + isMultiple.toString());
				LogUtil.debug(LOGNAME + "Question is: " + question);
				LogUtil.debug(LOGNAME + "Answers are: " + answers);
				LogUtil.debug(LOGNAME + "Votes are: " + votes);
				LogUtil.debug(LOGNAME + "Time is: " + time);
				LogUtil.debug(LOGNAME + "TotalVotes is: " + totalVotes);
				LogUtil.debug(LOGNAME + "Status is: " + status.toString());
				LogUtil.debug(LOGNAME + "DidNotVote is: " + didNotVote);
				LogUtil.debug(LOGNAME + "PublishToWeb is: " + publishToWeb.toString());
				LogUtil.debug(LOGNAME + "WebKey is: " + webKey);
				LogUtil.debug(LOGNAME + "--------------");
			}else{
				LogUtil.error(LOGNAME + "This PollObject is NULL.");
			}
		}
		
		public function generateStats():ArrayCollection{
			/*var returnCollection:ArrayCollection = new ArrayCollection();
			for (var i:int = 0; i < answers.length; i++){
				var pso:PollStatLineObject = new PollStatLineObject;
				pso.answer = answers[i].toString();
				pso.votes = votes[i].toString();
				//pso.percentage =
				if (totalVotes == 0){
					pso.percentage = "";
				}
				else{
					pso.percentage = Math.round(100*(votes[i]/totalVotes)) + "%";
				}
				returnCollection.addItem(pso);
			}
			return returnCollection;*/
			var buildArray:Array = generateStatArray();
			var returnCollection:ArrayCollection = new ArrayCollection(buildArray);
			return returnCollection;
		}
		
		public function generateStatArray():Array{
			var returnArray:Array = new Array();
			for (var i:int = 0; i < answers.length; i++){
				var percent:String;
				if (totalVotes == 0){
					percent = "";
				}
				else{
					percent = Math.round(100*(votes[i]/totalVotes)) + "%";
				}
				
				// Functioning proof-of-concept, do not remove!
				returnArray.push({answer: answers[i].toString(), votes: votes[i].toString(), percentage: percent});
				// Functioning proof-of-concept, do not remove!
				
				// Does not work: returnArray.push({{answerT}: answers[i].toString(), {votesT}: votes[i].toString(), {percentT}: percent});
				// Maybe doesn't work? 
				returnArray.push({"{answerT}": answers[i].toString(), "{votesT}": votes[i].toString(), "{percentT}": percent});
				
				
				/*returnArray.push({{ResourceUtil:getInstance().getString('bbb.polling.stats.answers')}: answers[i].toString(), 
								  {ResourceUtil:getInstance().getString('bbb.polling.stats.votes')}: votes[i].toString(), 
								  {ResourceUtil:getInstance().getString('bbb.polling.stats.percentage')}: percent});*/
			}
			// Adding "Did Not Vote"
			/*returnArray.push({ResourceUtil:getInstance().getString('bbb.polling.stats.answers'): ResourceUtil:getInstance().getString('bbb.polling.stats.didNotVote'), 
							  ResourceUtil:getInstance().getString('bbb.polling.stats.votes'): didNotVote.toString(), 
							  ResourceUtil:getInstance().getString('bbb.polling.stats.percentage'): ""});*/
			
			return returnArray;
		}
		
	}
}
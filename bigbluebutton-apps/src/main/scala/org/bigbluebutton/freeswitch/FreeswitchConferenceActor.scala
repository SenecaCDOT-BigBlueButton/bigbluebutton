package org.bigbluebutton.freeswitch

import scala.actors.Actor
import scala.actors.Actor._
import org.bigbluebutton.core.api._

case class FsVoiceUserJoined(userId: String, webUserId: String, 
                             conference: String, callerIdNum: String, 
                             callerIdName: String, muted: Boolean, 
                             speaking: Boolean)
               
case class FsVoiceUserLeft(userId: String, conference: String)
case class FsVoiceUserLocked(userId: String, conference: String, locked: Boolean)
case class FsVoiceUserMuted(userId: String, conference: String, muted: Boolean)
case class FsVoiceUserTalking(userId: String, conference: String, talking: Boolean)
case class FsRecording(conference: String, recordingFile: String, 
                            timestamp: String, recording: Boolean)

class FreeswitchConferenceActor(fsproxy: FreeswitchManagerProxy, bbbInGW: IBigBlueButtonInGW) extends Actor {

  private var confs = new scala.collection.immutable.HashMap[String, FreeswitchConference]
  
  def act() = {
	loop {
	  react {
	    case msg: MeetingCreated                     => handleMeetingCreated(msg)
	    case msg: MeetingEnded                       => handleMeetingEnded(msg)
	    case msg: UserJoined                         => handleUserJoined(msg)
	    case msg: UserLeft                           => handleUserLeft(msg)
	    case msg: MuteVoiceUser                      => handleMuteVoiceUser(msg)
	    case msg: EjectVoiceUser                     => handleEjectVoiceUser(msg)
	    case msg: StartRecording                     => handleStartRecording(msg)
	    case msg: StopRecording                      => handleStopRecording(msg)
	    case msg: FsRecording                        => handleFsRecording(msg)
	    case msg: FsVoiceUserJoined                  => handleFsVoiceUserJoined(msg)
	    case msg: FsVoiceUserLeft                    => handleFsVoiceUserLeft(msg)
	    case msg: FsVoiceUserLocked                  => handleFsVoiceUserLocked(msg)
	    case msg: FsVoiceUserMuted                   => handleFsVoiceUserMuted(msg)
	    case msg: FsVoiceUserTalking                 => handleFsVoiceUserTalking(msg)
	    case msg: UserJoinedVoice                    => handleUserJoinedVoice(msg)
	    case msg: UserLeftVoice                      => handleUserLeftVoice(msg)
	    case _ => // do nothing
	  }
	}
  }  
  
  private def handleMeetingCreated(msg: MeetingCreated) {
    if (! confs.contains(msg.meetingID)) {
//      println("FSConference rx meeting created for meeting id[" + msg.meetingID + "]")
      val fsconf = new FreeswitchConference(msg.voiceBridge,
                                            msg.meetingID, 
                                            msg.recorded)
      confs += fsconf.meetingId -> fsconf
    }
    
    fsproxy.getUsers(msg.voiceBridge)
  }
  
  private def handleMeetingEnded(msg: MeetingEnded) {
    val fsconf = confs.values find (c => c.meetingId == msg.meetingID)
    
    fsconf foreach (fc => {
      fsproxy.ejectUsers(fc.conferenceNum)
      confs -= fc.meetingId
    })
  }
  
  private def handleUserJoinedVoice(msg: UserJoinedVoice) {
    val fsconf = confs.values find (c => c.meetingId == msg.meetingID)
    
    fsconf foreach {fc => 
//      println("Web user has joined voice. mid[" + fc.meetingId + "] wid=[" + msg.user.userID + "], vid=[" + msg.user.voiceUser.userId + "]")
      fc.addUser(msg.user)
      if (fc.numUsersInVoiceConference == 1 && fc.recorded) {
        println("Meeting is recorded. Tell FreeSWITCH to start recording.")
        fsproxy.startRecording(fc.conferenceNum, fc.meetingId)
      }
    }
  }
  
  private def handleUserLeftVoice(msg: UserLeftVoice) {
    val fsconf = confs.values find (c => c.meetingId == msg.meetingID)
    
//    println("FreeswitchConferenceActor - handleUserLeftVoice mid=[" + msg.meetingID + "]")
    
    fsconf foreach {fc => 
      fc.addUser(msg.user)
//      println("Web user has left voice. mid[" + fc.meetingId + "] wid=[" + msg.user.userID + "], vid=[" + msg.user.voiceUser.userId + "]")
      if (fc.numUsersInVoiceConference == 0 && fc.recorded) {
        println("Meeting is recorded. No more users in voice conference. Tell FreeSWITCH to stop recording.")
        fsproxy.stopRecording(fc.conferenceNum)
      }
    }
  }
  
  private def handleUserJoined(msg: UserJoined) {
    val fsconf = confs.values find (c => c.meetingId == msg.meetingID)
    
    fsconf foreach (fc => {
//      println("Web user id joining meeting id[" + fc.meetingId + "] wid=[" + msg.user.userID + "]")
      fc.addUser(msg.user)
    })
  }
  
  private def handleUserLeft(msg: UserLeft) {
    val fsconf = confs.values find (c => c.meetingId == msg.meetingID)
    
    fsconf foreach (fc => {
      fc.removeUser(msg.user)
    })
  }
  
  private def handleMuteVoiceUser(msg: MuteVoiceUser) {
    val fsconf = confs.values find (c => c.meetingId == msg.meetingID)
//    println("Mute user request for wid[" + msg.userId + "] mute=[" + msg.mute + "]")    
    fsconf foreach (fc => {
      val user = fc.getWebUser(msg.userId)
      user foreach (u => {
        println("Muting user wid[" + msg.userId + "] mute=[" + msg.mute + "]") 
        fsproxy.muteUser(fc.conferenceNum, u.voiceUser.userId, msg.mute)
      })
    })    
  }
  
  private def handleEjectVoiceUser(msg: EjectVoiceUser) {
    val fsconf = confs.values find (c => c.meetingId == msg.meetingID)
    
    fsconf foreach (fc => {
      val user = fc.getWebUser(msg.userId)
      user foreach (u => {
        fsproxy.ejectUser(fc.conferenceNum, u.voiceUser.userId)
      })
    })    
  }
    
  private def handleStartRecording(msg: StartRecording) {
    
  }
    
  private def handleStopRecording(msg: StopRecording) {
    
  }  
  
  private def handleFsRecording(msg: FsRecording) {
    val fsconf = confs.values find (c => c.conferenceNum == msg.conference)
    fsconf foreach {fc => 
      bbbInGW.voiceRecording(fc.meetingId, msg.recordingFile, msg.timestamp, msg.recording) 
    }
  }
  
  private def sendNonWebUserJoined(meetingId: String, webUserId: String, 
      msg: FsVoiceUserJoined) {
//     println("FreeswitchConferenceActor:: Sending FsVoiceUserJoined for conference [" + 
//                msg.conference + "] user=[" + msg.callerIdName + "] userid=[" + webUserId + "]")
     bbbInGW.voiceUserJoined(meetingId, msg.userId, 
	              webUserId, msg.conference, msg.callerIdNum, msg.callerIdName,
	              msg.muted, msg.speaking)    
  }
  
  private def handleFsVoiceUserJoined(msg: FsVoiceUserJoined) {
//    println("FreeswitchConferenceActor:: Received FsVoiceUserJoined for conference [" + 
//                msg.conference + "] user=[" + msg.callerIdName + "] wid=[" + msg.webUserId + "]")
    val fsconf = confs.values find (c => c.conferenceNum == msg.conference)
    
    fsconf foreach (fc => {
	  fc.getWebUser(msg.webUserId) match {
	   case Some(user) => {
//         println("FreeswitchConferenceActor:: Found webuser for this user for conference [" + 
//                msg.conference + "] user=[" + msg.callerIdName + "] wid=[" + msg.webUserId + "]")	     
	     sendNonWebUserJoined(fc.meetingId, user.voiceUser.webUserId, msg)
	   }
	   case None => {
//	     println("FreeswitchConferenceActor:: Did not find webuser for this user for conference [" + 
//                msg.conference + "] user=[" + msg.callerIdName + "] wid=[" + msg.webUserId + "]")	
	     sendNonWebUserJoined(fc.meetingId, msg.userId, msg)
	   }
	  }
    })
  }
  
  private def handleFsVoiceUserLeft(msg: FsVoiceUserLeft) {
    val fsconf = confs.values find (c => c.conferenceNum == msg.conference)

    fsconf foreach (fc => {
      val user = fc.getVoiceUser(msg.userId) 
      user foreach (u => bbbInGW.voiceUserLeft(fc.meetingId, u.userID))
    })
  }
  
  private def handleFsVoiceUserLocked(msg: FsVoiceUserLocked) {
    val fsconf = confs.values find (c => c.conferenceNum == msg.conference)
    
    fsconf foreach (fc => {
      val user = fc.getVoiceUser(msg.userId)   
      user foreach (u => bbbInGW.voiceUserLocked(fc.meetingId, u.userID, msg.locked))
    })    
  }
  
  private def handleFsVoiceUserMuted(msg: FsVoiceUserMuted) {
    val fsconf = confs.values find (c => c.conferenceNum == msg.conference)

//    println("Rx voice user muted for cnum=[" + msg.conference + "] vid[" + msg.userId + "] mute=[" + msg.muted + "]")    
    fsconf foreach (fc => {
      val user = fc.getVoiceUser(msg.userId) 
//      println("Rx voice user muted for mid=[" + fc.meetingId + "] vid[" + msg.userId + "] mute=[" + msg.muted + "]") 
      user foreach (u => bbbInGW.voiceUserMuted(fc.meetingId, u.userID, msg.muted))
    })      
  }
  
  private def handleFsVoiceUserTalking(msg: FsVoiceUserTalking) {
    val fsconf = confs.values find (c => c.conferenceNum == msg.conference)
    
    fsconf foreach (fc => {
      val user = fc.getVoiceUser(msg.userId) 
//      println("Rx voice user talking for vid[" + msg.userId + "] mute=[" + msg.talking + "]") 
      user foreach (u => bbbInGW.voiceUserTalking(fc.meetingId, u.userID, msg.talking))
    })      
  }
}
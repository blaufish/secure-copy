package org.securecopy.actors;

import org.securecopy.messages.Message;

public abstract class ReliableActor {
	/** runs in a single thread.
	 * 
	 * @param message the message to be acted upon
	 * @return true if dependent actors should also receive message, false if an error occurred.
	 */
	public abstract void onReceive(Message message);
}

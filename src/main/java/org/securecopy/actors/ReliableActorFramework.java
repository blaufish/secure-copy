package org.securecopy.actors;

import java.util.concurrent.ArrayBlockingQueue;

import org.securecopy.messages.Message;
import org.securecopy.messages.TerminateMessage;

public class ReliableActorFramework {
	private final ActorRunnable[] runners;
	private final Thread[] threads;

	public ReliableActorFramework(ReliableActor... actors) {
		runners = new ActorRunnable[actors.length];
		threads = new Thread[actors.length];
		for (int i = 0; i < actors.length; i++) {
			runners[i] = new ActorRunnable(actors[i]);
			threads[i] = new Thread(runners[i]);
		}
	}

	public void start() {
		for (Thread thread : threads)
			thread.start();
	}

	public void stop() {
		post(new TerminateMessage());
		for (Thread thread : threads) {
			try {
				thread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void post(Message message) {
		for (ActorRunnable runner : runners) {
			retryPost: for (int i = 0; i < 10; i++) {
				try {
					runner.queue.put(message);
					break retryPost;
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	private static class ActorRunnable implements Runnable {
		private final ReliableActor actor;
		private final ArrayBlockingQueue<Message> queue;

		private ActorRunnable(ReliableActor actor) {
			this.actor = actor;
			this.queue = new ArrayBlockingQueue<Message>(1000);
		}

		public void run() {
			Message msg;
			do {
				try {
					msg = queue.take();
					actor.onReceive(msg);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
					msg = null;
				}
			} while (!(msg instanceof TerminateMessage));

		}
	}

}

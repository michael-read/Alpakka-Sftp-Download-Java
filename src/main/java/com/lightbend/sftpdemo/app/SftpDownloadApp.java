package com.lightbend.sftpdemo.app;

import akka.NotUsed;
import akka.actor.typed.ActorRef;
import akka.actor.typed.ActorSystem;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AskPattern;
import akka.actor.typed.javadsl.Behaviors;
import com.lightbend.sftpdemo.actors.SftpDownloadActor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

public class SftpDownloadApp {
    private static final Logger log = LoggerFactory.getLogger(SftpDownloadApp.class);

    public static Behavior<NotUsed> rootBehavior() {
        return Behaviors.setup(context -> {

            ActorRef<SftpDownloadActor.Command> downloadActor = context.spawn(SftpDownloadActor.create(), "SftpDownloadActor");
            String sourceDir = context.getSystem().settings().config().getString("app.source-dir");
            String targetDir = context.getSystem().settings().config().getString("app.target-dir");

            CompletionStage<SftpDownloadActor.TransferResponse> result =
                    AskPattern.ask(
                            downloadActor,
                            (ActorRef<SftpDownloadActor.TransferResponse> replyTo) -> new SftpDownloadActor.StartTransfer(sourceDir, targetDir, replyTo),
                            Duration.ofMinutes(1),
                            context.getSystem().scheduler());

            result.whenComplete(
                    (reply, failure) -> {
                        if (reply != null) {
                            log.info("The file transfer result: Success: {}, Message: {}", reply.isSuccess(), reply.getResponse());
                        }
                        else {
                            log.error("The file transfer failed: {}", failure.getMessage(), failure);
                        }
                        context.getSystem().terminate();
                    });

            return Behaviors.empty();
        });
    }

    public static void main(String[] args) {
        ActorSystem<NotUsed> system = ActorSystem.create(rootBehavior(), "SftpDownloadApp");
        system.getWhenTerminated();
    }
}

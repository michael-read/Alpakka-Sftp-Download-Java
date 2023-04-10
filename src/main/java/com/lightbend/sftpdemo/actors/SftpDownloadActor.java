package com.lightbend.sftpdemo.actors;

import akka.actor.typed.ActorRef;
import akka.actor.typed.Behavior;
import akka.actor.typed.javadsl.AbstractBehavior;
import akka.actor.typed.javadsl.ActorContext;
import akka.actor.typed.javadsl.Behaviors;
import akka.actor.typed.javadsl.Receive;
import akka.japi.Pair;
import akka.stream.IOResult;
import akka.stream.alpakka.ftp.FtpCredentials;
import akka.stream.alpakka.ftp.SftpSettings;
import akka.stream.alpakka.ftp.javadsl.Sftp;
import akka.stream.javadsl.FileIO;
import akka.stream.javadsl.Sink;
import com.lightbend.sftpdemo.app.SftpDownloadApp;
import com.typesafe.config.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CompletionStage;

public class SftpDownloadActor extends AbstractBehavior<SftpDownloadActor.Command> {
    private final Logger log = LoggerFactory.getLogger(SftpDownloadActor.class);

    public interface Command {}

    public static class StartTransfer implements Command {
        String sourceDir;
        String targetDir;
        ActorRef<TransferResponse> replyTo;

        public StartTransfer(String sourceDir, String targetDir, ActorRef<TransferResponse> replyTo) {
            this.sourceDir = sourceDir;
            this.targetDir = targetDir;
            this.replyTo = replyTo;
        }

    };
    public static enum Stop implements Command {INSTANCE};
    public static class TransferResponse implements Command {
        private final boolean success;
        private final String response;

        public TransferResponse(boolean success, String response) {
            this.success = success;
            this.response = response;
        }
        public boolean isSuccess() { return success; }
        public String getResponse() {
            return response;
        }
    }

    public static Behavior<Command> create() {
        return Behaviors.setup(ctx -> new SftpDownloadActor(ctx));
    }

    private SftpDownloadActor(ActorContext<Command> context) {
        super(context);
    }

    @Override
    public Receive<Command> createReceive() {
        return newReceiveBuilder()
                .onMessage(StartTransfer.class, this::onStartTransfer)
                .onMessageEquals(Stop.INSTANCE, this::onStop)
                .build();
    }

    private Behavior<Command> onStartTransfer(StartTransfer msg) {

        final int parallelism = 5;
        SftpSettings sftpSettings = createSftpSettings(getContext().getSystem().settings().config());

        final Path targetDir = Paths.get(msg.targetDir);

        final CompletionStage<List<Pair<String, IOResult>>> fetchedFiles =
                Sftp.ls(msg.sourceDir, sftpSettings)
                        .filter(sftpFile -> sftpFile.isFile())
                        .mapAsyncUnordered(
                                parallelism,
                                sftpFile -> {
                                    final Path localPath = targetDir.resolve(sftpFile.name());
                                    final CompletionStage<IOResult> fetchFile =
                                            Sftp.fromPath(sftpFile.path(), sftpSettings.withMaxUnconfirmedReads(64))
                                                    .runWith(FileIO.toPath(localPath), getContext().getSystem());
                                    return fetchFile.thenApply(
                                            ioResult -> Pair.create(sftpFile.path(), ioResult));
                                }
                        )
                        .runWith(Sink.seq(), getContext().getSystem());

        fetchedFiles
                .whenComplete(
                        (res, ex) -> {
                            TransferResponse response = null;
                            if (res != null) {
                                if (!res.isEmpty()) {
                                    res.forEach( resultPair -> {
                                        log.info("File: {}, outcome {}", resultPair.first(), resultPair.second());
                                    });
                                    String respMsg = "all files fetched";
                                    // context is gone by now so use global
                                    log.info(respMsg);
                                    response = new TransferResponse(true, respMsg);
                                }
                                else {
                                    String respMsg = "errors occured: ";
                                    log.error(respMsg);
                                    response = new TransferResponse(false, respMsg);
                                }
                            }
                            else {
                                String respMsg = "errors occured: " + ex.getMessage();
                                log.error(respMsg, ex);
                                response = new TransferResponse(false, respMsg);
                            }
                            msg.replyTo.tell(response);
                        });

        return Behaviors.same();
    }

    private Behavior<Command> onStop() {
        return Behaviors.stopped();
    }

    private SftpSettings createSftpSettings(Config config) {
        String hostname = config.getString("app.host");
        int port = config.getInt("app.port");
        String username = config.getString("app.username");
        String password = config.getString("app.password");
        SftpSettings sftpSettings = null;

        try {
            sftpSettings = SftpSettings.create(InetAddress.getByName(hostname))
                    .withPort(port)
                    .withCredentials(FtpCredentials.create(username, password))
                    .withStrictHostKeyChecking(false);
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
        return sftpSettings;
    }
}

FROM discoenv/javabase

ADD target/metadactyl-standalone.jar /home/iplant/
ADD conf/main/log4j2.xml /home/iplant/
USER root
RUN chown -R iplant:iplant /home/iplant/
RUN mkdir -p /etc/iplant/de/crypto
RUN touch /etc/iplant/de/crypto/pubring.gpg
RUN touch /etc/iplant/de/crypto/random_seed
RUN touch /etc/iplant/de/crypto/secring.gpg
RUN touch /etc/iplant/de/crypto/trustdb.gpg
RUN chown -R iplant:iplant /etc/iplant/de/crypto
VOLUME ["/etc/iplant/de"]
USER iplant
ENTRYPOINT ["java", "-cp", ".:metadactyl-standalone.jar:/home/iplant/", "metadactyl.core"]
CMD ["--help"]

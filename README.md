# wo-island
useless discord bot


## Config
See `src/main/resources/application.properties` to set all necessary properties:

### API Token
Enter api token for your discord bot (see [discord developer](https://discord.com/developers/docs/intro) portal to create one), e.g.:
```
discord.client.token.value=12345abcdef
```
Alternatively you can set the environment variable `WO_ISLAND_DISCORD_CLIENT_TOKEN` in the eventual runtime environment (the container if dockerized). 
The environment variable has precedence over the application property.

### Channels and keywords
The bot responds to certain channels and keywords. You can specify a list for each property, e.g.:
```
events.wo-island.channel-ids={"channelId1", "channelId2", "channelId3"}
events.wo-island.key-words={"keyword1", "keyword2"}
```
With the above configuration the bot would only respond to messages in channels with channelId1, channelId2 and channelId3 that contains keyword1 or keyword2. 
To find a channels id right click it on your discord server and select "copy link". You'll find the id as parameter of that link.

### User mentioned
Additionally, you can specify for the bot to react when certain users are mentioned. This is independent of channel ids.

```
events.user-mentioned.emoji.codepoints=insert-code-points
events.user-mentioned.user-names={"user-name1, user-name2"}
```

## Build
Run to build and containerize the app via jib maven plugin.
``` 
mvn compile jib:build -Djib.image.path=imagePath/imageName:tag
```
`-Djib.image.path` specifies the location of your image registry and image name. To push to an external registry make sure to do a `docker login` beforehand.

Alternatively you can use `.mvnw` and/or provide login credentials by cli parameter like this:
```
./mvnw jib:build -Djib.image.path=imagePath/imageName:tag -Djib.to.auth.username=***** -Djib.to.auth.password=*****
```

## Run
Run the resulting image in a container environment of your choosing. Don't forget to mount the environment variable for the api token if you have not set it via application properties.

## Contact
Feedback or questions? Contact @timcammann. wo island?
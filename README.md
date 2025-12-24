# wo-island
useless discord bot

## Config
See `src/main/resources/application.properties` to set all necessary properties:

### API Token
Enter api token for your discord bot (see [discord developer](https://discord.com/developers/docs/intro) portal to create one), e.g.:
```
discord.client.token.value=12345abcdef
```

### Feature: React to messages

#### Channels and keywords
The bot responds to certain channels and keywords. You can specify a list for each property, e.g.:
```
events.wo-island.channel-ids={"channelId1", "channelId2", "channelId3"}
events.wo-island.key-words={"keyword1", "keyword2"}
```
With the above configuration, the bot would only respond to messages in channels with channelId1, channelId2 and channelId3 that contains keyword1 or keyword2. 
To find a channel's id right-click it on your discord server and select "copy link". You'll find the id as parameter of that link.

#### User mentioned
Additionally, you can specify for the bot to react when certain users are mentioned. This is independent of channel ids.

```
events.user-mentioned.emoji.codepoints=insert-code-points
events.user-mentioned.user-names={"user-name1, user-name2"}
```

#### User posted image
Let the bot react to an image posted by specified users in specified channels with:
```
events.user-posted-image.emoji.id=123456
events.user-posted-image.emoji.name=example-name
events.user-posted-image.user-names={"example-user-name", "example-user-name-2"}
events.user-posted-image.channel-ids={"456789", "567890"}
```

### Feature: Create reaction ranking
Who received the most reactions (by the bot or any other user)? 

Configure what emojis you'd like to track with:
```
events.reaction.ranking.tracked-emojis.custom.names={"example-emoji", "another"}
events.reaction.ranking.tracked-emojis.utf8.code-points={"U+1F3DD", "U+12345"}
```
For server specific (custom) emojis enter the names used to type the emojis (for `:example-emoji:` the name is `example-emoji`).
To track regular emojis, enter the UTF8 code points. For example `U+1F3DD` for the island emoji: https://www.emojiall.com/en/emoji/%F0%9F%8F%9D.

Enter your server's ID for the application command (slash command) to be registered:
```
events.reaction.ranking.command-name=reaction-ranking
```
Then use (in this case) `/reaction-ranking` to have the bot create and post the ranking. Use the option `timeframe` to select the timeframe to analyse. 

The database that tracks all configured emoji-reactions is an embedded h2 database located under `/database`.

## Build
Run to build and containerize the app via jib maven wrapper.
``` 
./mvnw -U clean compile jib:build -Djib.image.path=imagePath/imageName:tag
```
`-Djib.image.path` specifies the location of your image registry and image name. To push to an external registry make sure to do a `docker login` beforehand.

Windows:
```
.\mvnw.cmd -U clean compile jib:build -Dimage=imagePath/imageName:tag
```

Alternatively, you can provide login credentials by cli parameter like this:
```
./mvnw -U clean compile jib:build -Djib.image.path=imagePath/imageName:tag -Djib.to.auth.username=***** -Djib.to.auth.password=*****
```

## Run 
Run the resulting image in a container environment of your choosing. Don't forget to mount the environment variable for the api token if you have not set it via application properties. Add a volume for `/database` if you'd like to persist the database across container recreation.

## Contact
Feedback or questions? Contact @timcammann. wo island?
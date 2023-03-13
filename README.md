# Ultralight Fabric
An example implementation of the ultralight html renderer into 1.19.3 minecraft using the fabric toolchain

## How?
Ultralight is a bit picky, and I can't explain it in full here, but the source code is very well documented. Everything is explained there.

## Can I use this?
Generally yes, but you might have to fix certain things if you don't use linux. I only tested this on x64 linux, your mileage may vary.

If you need to replace the drivers, the commit hash they're built on is `5011dbf`. All files in `src/main/resources/natives` will be extracted at runtime, to a cache location, and provided to ultralight. All files in `src/main/resources/ul-resources` will be extracted as well, and provided to ultralight as resources.

### How can I use this?
You can just use the same setup for the most part, the `HtmlScreen` class is an example implementation of ultralight into the Screen api.

`new HtmlScreen("file:///local/url.html")` will construct a new HtmlScreen pointing to local/url.html, relative to the current working directory. I have no idea how to specify an absolute path, that's just a thing ultralight does, apparently.

Things like `new HtmlScreen("https://google.com")` work as well, but modern and complex websites (like youtube, steam, etc) have limited functionality when rendered with ultralight. Google works fine, except for 1 or 2 minor css fuckups. 

Remember to cache the screens you create. You don't need to make a new screen each time you want to open one. You can just reuse the instance, and `HtmlScreen` is made to do exactly that.

## How long did this take you?
To implement? about 4-5 hours

To actually get to a point where I've had enough knowledge to implement it? overall, about 2 weeks of research and fumbling around

# lein-invoke

A [Leiningen](http://leiningen.org/) plugin for invoking and testing lein tasks.

## Usage

Create a `invoke.clj` in the root a sample project like this:

```clj
[[:exec "git" "init"]
 [:lein "uberjar"]
 [:exists? "target/myapp.jar"]]
```

Then run:

```
$ lein invoke path/to/app
```

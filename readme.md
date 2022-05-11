# Kotlin Reverse Proxy PoC

This repo attempts to demonstrate how coroutines can be used to 
maintain state for a HTTP request while its contents are being proxied
over a websocket.

## Usage
    
Big file:

```shell
wget --verbose http://127.0.0.1:8080/22.04/ubuntu-22.04-desktop-amd64.iso
```

Small file:

```shell
wget --verbose http://127.0.0.1:8080/icons/folder.gif
```

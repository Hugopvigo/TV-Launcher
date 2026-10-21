#!/usr/bin/env bash
# Datos del Mi TV Stick y localizacion de su IP.
#
# El stick (que en casa es el "HA TV dormitorio") cambia de IP con el DHCP,
# asi que se busca por su MAC en la tabla ARP del router. Si esta en
# suspension profunda no aparece en ningun sitio: entonces se usa la ultima
# IP conocida.
#
#   source tools/tv.sh
#   adb_tv shell getprop ro.boot.wifimac
#
# Variables:
#   TV_MAC      MAC del stick (la de Wi-Fi, la de fabrica)
#   TV_LAST_IP  ultima IP que se le vio
#   TV_NAME     como se le llama en casa
#   TV_SERIAL   "ip:5555", listo para adb

TV_NAME="HA TV dormitorio"
TV_MAC="6C-0D-C4-C7-5F-F7"
TV_LAST_IP="192.168.3.69"

# La IP del stick segun la tabla ARP del router.
#
# El host Windows es quien ve la LAN (WSL va por NAT), asi que se pregunta
# primero ahi y luego al ARP local.
tv_find_ip() {
    local mac arp
    mac="${TV_MAC//-/}"
    mac="${mac//:/}"
    if [ -x "/mnt/c/Windows/System32/arp.exe" ]; then
        arp=$(/mnt/c/Windows/System32/arp.exe -a 2>/dev/null)
    else
        arp=$(arp -a 2>/dev/null)
    fi
    printf '%s\n' "$arp" \
        | tr 'A-F' 'a-f' \
        | tr '-' ':' \
        | grep -i "$mac" \
        | awk '{print $1; exit}'
}

# "ip:5555" del stick, con la IP que se le vea ahora o la ultima conocida.
tv_serial() {
    local ip
    ip="$(tv_find_ip)"
    if [ -z "$ip" ]; then
        ip="$TV_LAST_IP"
    fi
    printf '%s:5555' "$ip"
}

# La IP actual, para dejarla anotada en los scripts.
tv_ip() {
    local ip
    ip="$(tv_find_ip)"
    if [ -z "$ip" ]; then
        ip="$TV_LAST_IP"
    fi
    printf '%s' "$ip"
}

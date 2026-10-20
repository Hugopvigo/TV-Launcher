#!/usr/bin/env python3
"""
Genera la imagen de cabecera del proyecto.

Filosofia: "Luz de Fondo" (docs/canvas/luz-de-fondo.md).

Composicion:
  - Fondo grafito con un halo radial en la esquina superior izquierda.
  - Izquierda: display en Outfit ExtraLight, cuerpo en Outfit Light.
  - Derecha: la captura real del launcher, recortada con esquinas
    redondeadas y un halo difuso detras (la luz que emite).
  - Abajo: datos tecnicos en monoespaciada fina, como marcas de campo.

El tamano del display se ajusta al hueco disponible: nada se sale del
lienzo y nada se solapa.
"""
import os
from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
FONTS = os.path.join(ROOT, "launcher", "app", "src", "main", "res", "font")
SHOTS = os.path.join(ROOT, "docs", "capturas")
OUT = os.path.join(ROOT, "docs", "capturas", "00-cabecera.png")

W, H = 2000, 1150
M = 96  # margen

# Paleta: monocroma y fria, con un unico acento (rojo de la interfaz).
BG = (8, 9, 12)
HALO = (20, 22, 28)
BONE = (245, 245, 247)
MIST = (161, 161, 166)
FAINT = (110, 110, 115)
LINE = (30, 32, 38)
ACCENT = (255, 0, 0)

MONO = "/usr/share/fonts/truetype/dejavu/DejaVuSansMono.ttf"


def font(name, size):
    return ImageFont.truetype(os.path.join(FONTS, name), size)


def mono(size):
    return ImageFont.truetype(MONO, size)


def radial_halo(w, h, cx, cy, radius, color, strength=255):
    """Halo radial: se dibuja en un lienzo propio y se compone encima."""
    layer = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    d = ImageDraw.Draw(layer)
    steps = 100
    for i in range(steps, 0, -1):
        t = i / steps
        r = radius * t
        a = int(strength * (1 - t) ** 2.1)
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=color + (a,))
    return layer.filter(ImageFilter.GaussianBlur(60))


def rounded(img, radius):
    mask = Image.new("L", img.size, 0)
    ImageDraw.Draw(mask).rounded_rectangle([0, 0, img.size[0] - 1, img.size[1] - 1],
                                           radius=radius, fill=255)
    out = img.convert("RGBA")
    out.putalpha(mask)
    return out


def fit_display(draw, text, path, max_w, max_h, start=260):
    """Mayor tamano con el que el texto cabe en el hueco dado."""
    size = start
    while size > 40:
        f = font(path, size)
        box = draw.textbbox((0, 0), text, font=f)
        w = box[2] - box[0]
        h = box[3] - box[1]
        if w <= max_w and h <= max_h:
            return f, size
        size -= 2
    return font(path, 40), 40


def main():
    canvas = Image.new("RGBA", (W, H), BG + (255,))
    canvas.alpha_composite(radial_halo(W, H, int(W * 0.16), int(H * 0.06),
                                       int(W * 0.58), HALO, 235))
    canvas.alpha_composite(radial_halo(W, H, int(W * 0.88), int(H * 0.96),
                                       int(W * 0.42), (12, 13, 17), 190))
    d = ImageDraw.Draw(canvas)

    # ---------- retícula: solo marcas de esquina, no líneas completas ----------
    for (ax, ay, dx, dy) in ((M, M, 1, 1), (W - M, M, -1, 1),
                             (M, H - M, 1, -1), (W - M, H - M, -1, -1)):
        d.line([(ax, ay), (ax + dx * 26, ay)], fill=LINE, width=1)
        d.line([(ax, ay), (ax, ay + dy * 26)], fill=LINE, width=1)

    # ---------- captura (derecha): se dimensiona primero para saber el hueco ----------
    shot = Image.open(os.path.join(SHOTS, "03-launcher-inicio.png")).convert("RGB")
    sw = 880
    sh = int(shot.height * sw / shot.width)
    shot = shot.resize((sw, sh), Image.LANCZOS)
    sx = W - M - sw
    sy = (H - sh) // 2 - 10

    # ---------- titulo (izquierda) ----------
    x0 = M
    col_w = sx - M - 80          # hueco real hasta la captura
    top = 150

    f_tv, s_tv = fit_display(d, "TV", "outfit_extralight.ttf", col_w, 230, 240)
    f_ln, s_ln = fit_display(d, "LAUNCHER", "outfit_light.ttf", col_w, 200, 240)

    d.text((x0 - 4, top), "TV", font=f_tv, fill=BONE)
    d.text((x0 - 3, top + s_tv + 14), "LAUNCHER", font=f_ln, fill=BONE)

    y_after = top + s_tv + 14 + s_ln + 26

    # acento: el punto rojo, unica excepcion cromatica
    d.ellipse([x0 - 4, y_after, x0 + 16, y_after + 20], fill=ACCENT + (255,))

    # subtitulo
    f_sub = font("outfit_light.ttf", 30)
    d.text((x0, y_after + 44), "Xiaomi Mi TV Stick  ·  Android 9  ·  1 GB de RAM",
           font=f_sub, fill=MIST)

    # ---------- datos: marcas de campo ----------
    f_num = font("outfit_medium.ttf", 42)
    f_lbl = mono(19)
    yy = y_after + 130
    for num, label in (("21", "paquetes desactivados"),
                       ("07", "apps desinstaladas"),
                       ("+400", "MB liberados en /data")):
        d.text((x0, yy), num, font=f_num, fill=BONE)
        d.text((x0 + 160, yy + 14), label, font=f_lbl, fill=FAINT)
        d.line([(x0, yy + 62), (x0 + 470, yy + 62)], fill=LINE, width=1)
        yy += 84

    # pie tecnico
    f_note = mono(16)
    d.text((x0, H - M + 4),
           "es.hugoperezvigo.tvlauncher   ·   v1.0   ·   release firmado   ·   1.6 MB",
           font=f_note, fill=FAINT)

    # ---------- indice lateral, pegado al margen derecho ----------
    f_idx = mono(16)
    for i, t in enumerate(("01  inicio", "02  rejilla", "03  foco")):
        w = d.textlength(t, font=f_idx)
        d.text((W - M - w, 150 + i * 26), t, font=f_idx, fill=(78, 79, 86, 255))

    # ---------- la pantalla: halo detras, luego el rectangulo ----------
    glow = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    g = Image.new("RGBA", (sw + 140, sh + 140), (0, 0, 0, 0))
    ImageDraw.Draw(g).rounded_rectangle([70, 70, sw + 70, sh + 70],
                                         radius=34, fill=(140, 150, 185, 62))
    glow.alpha_composite(g.filter(ImageFilter.GaussianBlur(50)), (sx - 70, sy - 70))
    canvas.alpha_composite(glow)

    canvas.alpha_composite(rounded(shot, 26), (sx, sy))
    d2 = ImageDraw.Draw(canvas)
    d2.rounded_rectangle([sx, sy, sx + sw - 1, sy + sh - 1], radius=26,
                         outline=(56, 58, 68, 255), width=1)

    # marca de referencia dentro de la pantalla
    f_mark = mono(14)
    d2.text((sx + 16, sy + 14), "aquaman · 1920×1080 · API 28", font=f_mark,
            fill=(190, 190, 196, 165))

    canvas.convert("RGB").save(OUT, quality=96)
    print("escrito:", OUT, os.path.getsize(OUT), "bytes")


if __name__ == "__main__":
    main()

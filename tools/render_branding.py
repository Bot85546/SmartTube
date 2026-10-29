#!/usr/bin/env python3
"""Generate original PiTube icon and TV banner assets with Pillow."""
from pathlib import Path
from PIL import Image, ImageDraw, ImageFilter, ImageFont

OUT = Path(__file__).resolve().parents[1] / "smarttubetv/src/main/res/mipmap-nodpi"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf"
S = 3


def unit(value):
    return round(value * S)


def background(width, height, rounded=0):
    im = Image.new("RGBA", (unit(width), unit(height)))
    pixels = im.load()
    for y in range(im.height):
        for x in range(im.width):
            t = .30*x/im.width + .70*y/im.height
            a, b = (28, 31, 40), (8, 9, 13)
            pixels[x, y] = tuple(round(a[i]*(1-t)+b[i]*t) for i in range(3)) + (255,)
    if rounded:
        mask = Image.new("L", im.size)
        ImageDraw.Draw(mask).rounded_rectangle((0, 0, im.width-1, im.height-1), unit(rounded), fill=255)
        im.putalpha(mask)
    return im


def add_glow(im, x, y, radius):
    glow = Image.new("RGBA", im.size)
    ImageDraw.Draw(glow).ellipse((unit(x-radius), unit(y-radius), unit(x+radius), unit(y+radius)), fill=(120, 149, 218, 55))
    im.alpha_composite(glow.filter(ImageFilter.GaussianBlur(unit(radius*.55))))


def play_mark(im, cx, cy, size):
    d = ImageDraw.Draw(im)
    w, h = size, size*.68
    d.rounded_rectangle((unit(cx-w/2), unit(cy-h/2), unit(cx+w/2), unit(cy+h/2)),
                        radius=unit(size*.22), fill=(222, 230, 247, 255))
    d.polygon([(unit(cx-size*.11), unit(cy-size*.185)),
               (unit(cx+size*.21), unit(cy)),
               (unit(cx-size*.11), unit(cy+size*.185))], fill=(16, 18, 25, 255))


def save(im, name, w, h):
    im.resize((w, h), Image.Resampling.LANCZOS).save(OUT / name)


def main():
    OUT.mkdir(parents=True, exist_ok=True)

    icon = background(320, 320, 66)
    add_glow(icon, 235, 90, 95)
    ImageDraw.Draw(icon).rounded_rectangle((unit(10), unit(10), unit(309), unit(309)),
                                          radius=unit(58), outline=(196, 209, 240, 75), width=unit(2))
    play_mark(icon, 160, 160, 190)
    save(icon, "app_icon.png", 320, 320)
    save(icon, "app_icon_alt.png", 320, 320)

    banner = background(320, 180, 10)
    add_glow(banner, 275, 40, 90)
    play_mark(banner, 57, 90, 73)
    d = ImageDraw.Draw(banner)
    d.text((unit(109), unit(60)), "PiTube", font=ImageFont.truetype(FONT, unit(39)),
           fill=(246, 250, 255, 255))
    d.text((unit(110), unit(111)), "A CLEARER WAY TO WATCH.",
           font=ImageFont.truetype(FONT, unit(8)), fill=(172, 184, 207, 255))
    save(banner, "app_banner.png", 320, 180)

    header = Image.new("RGBA", (unit(320), unit(180)))
    play_mark(header, 160, 90, 180)
    for name in ("app_logo.png", "app_logo_semi_grey.png", "app_logo_semi_red.png"):
        save(header, name, 320, 180)


if __name__ == "__main__":
    main()

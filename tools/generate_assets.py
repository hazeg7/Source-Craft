import json
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src" / "main" / "resources"
ASSETS = RES / "assets" / "sourcecraft"
DATA = RES / "data" / "sourcecraft"
VERIFY = ROOT / "verify_out"

# Cool dark-slate + electric cyan + mint + coral palette (strictly zero yellow/gold/amber/cream)
BG_DARK = (11, 16, 27, 255)
SLATE_DEEP = (15, 23, 42, 255)
SLATE_MID = (30, 41, 59, 255)
SLATE_LIGHT = (51, 65, 85, 255)
STEEL_EDGE = (71, 85, 105, 255)
ICE_WHITE = (226, 232, 240, 255)
CYAN = (34, 211, 238, 255)
CYAN_BRIGHT = (103, 232, 249, 255)
CYAN_DIM = (22, 78, 99, 255)
MINT = (52, 211, 153, 255)
MINT_DIM = (6, 95, 70, 255)
CORAL = (248, 113, 113, 255)
CORAL_DIM = (127, 29, 29, 255)
TRANSPARENT = (0, 0, 0, 0)


def ensure_dirs():
    for rel in [
        ASSETS / "textures" / "block",
        ASSETS / "textures" / "item",
        ASSETS / "blockstates",
        ASSETS / "models" / "block",
        ASSETS / "models" / "item",
        ASSETS / "items",
        ASSETS / "lang",
        DATA / "loot_table" / "blocks",
        VERIFY,
    ]:
        rel.mkdir(parents=True, exist_ok=True)


def make_icon():
    size = 256
    img = Image.new("RGBA", (size, size), BG_DARK)
    draw = ImageDraw.Draw(img)

    # Subtle grid
    for g in range(16, size, 24):
        draw.line([(g, 12), (g, size - 12)], fill=(20, 30, 50, 255), width=1)
        draw.line([(12, g), (size - 12, g)], fill=(20, 30, 50, 255), width=1)

    # Outer precision frame
    draw.rectangle([8, 8, size - 9, size - 9], outline=STEEL_EDGE, width=3)
    draw.rectangle([16, 16, size - 17, size - 17], outline=CYAN_DIM, width=2)

    # Angled 45-degree surf ramp silhouette
    ramp_poly = [(36, 204), (176, 64), (218, 64), (218, 204)]
    draw.polygon(ramp_poly, fill=SLATE_MID, outline=STEEL_EDGE)
    # Bevel highlights on ramp face
    draw.line([(36, 204), (176, 64)], fill=CYAN, width=6)
    draw.line([(176, 64), (218, 64)], fill=CYAN_BRIGHT, width=4)
    draw.line([(48, 204), (188, 64)], fill=SLATE_LIGHT, width=3)

    # Mint & cyan 45-degree surf velocity chevrons floating cleanly above the ramp face
    for idx, (cx, cy) in enumerate([(72, 134), (104, 102), (136, 70)]):
        col = MINT if idx == 2 else (CYAN_BRIGHT if idx == 1 else CYAN)
        # Chevron pointing up-right along the 45-degree surf vector (+1, -1)
        tip = (cx + 12, cy - 12)
        left_wing = (cx - 10, cy - 8)
        inner = (cx + 2, cy - 2)
        right_wing = (cx + 8, cy + 10)
        draw.polygon([left_wing, tip, right_wing, inner], fill=col)

    # Corner calibration marks
    draw.rectangle([20, 20, 44, 24], fill=CYAN)
    draw.rectangle([20, 20, 24, 44], fill=CYAN)
    draw.rectangle([size - 44, size - 24, size - 20, size - 20], fill=MINT)
    draw.rectangle([size - 24, size - 44, size - 20, size - 20], fill=MINT)

    img.save(ASSETS / "icon.png")


def make_16x16_textures():
    tex_dir = ASSETS / "textures" / "block"
    item_tex_dir = ASSETS / "textures" / "item"
    previews = []

    def new_tile(fill=SLATE_DEEP):
        im = Image.new("RGBA", (16, 16), fill)
        return im, ImageDraw.Draw(im)

    # 1. surf_ramp_side
    im, d = new_tile(SLATE_DEEP)
    d.rectangle([0, 0, 15, 15], outline=STEEL_EDGE)
    for y in (4, 8, 12):
        d.line([(1, y), (14, y)], fill=SLATE_MID)
    for x in (4, 8, 12):
        d.line([(x, 1), (x, 14)], fill=SLATE_MID)
    d.point([(2, 2), (13, 2), (2, 13), (13, 13)], fill=CYAN_DIM)
    im.save(tex_dir / "surf_ramp_side.png")
    previews.append(("surf_ramp_side", im))

    # 2..4 surf_ramp_top_30 / 45 / 60
    for deg, accent, stripe_w in [(30, CYAN_DIM, 2), (45, CYAN, 2), (60, MINT, 3)]:
        im, d = new_tile(SLATE_MID)
        d.rectangle([0, 0, 15, 15], outline=STEEL_EDGE)
        for y in range(2, 14, 4):
            d.line([(2, y), (13, y)], fill=SLATE_LIGHT)
        # Edge guide rails
        d.rectangle([1, 1, stripe_w, 14], fill=accent)
        d.rectangle([15 - stripe_w, 1, 14, 14], fill=accent)
        # Flow chevrons pointing along slope (-Z = up)
        for cy in (4, 9):
            d.line([(5, cy + 2), (7, cy), (8, cy), (10, cy + 2)], fill=CYAN_BRIGHT if deg != 60 else MINT)
        name = f"surf_ramp_top_{deg}"
        im.save(tex_dir / f"{name}.png")
        previews.append((name, im))

    # 5. pad_side (highlight both top and bottom 2px strips so 2px pads always show cyan trim)
    im, d = new_tile(SLATE_DEEP)
    d.rectangle([0, 0, 15, 15], outline=STEEL_EDGE)
    d.line([(0, 0), (15, 0)], fill=CYAN)
    d.line([(0, 1), (15, 1)], fill=STEEL_EDGE)
    d.line([(0, 14), (15, 14)], fill=STEEL_EDGE)
    d.line([(0, 15), (15, 15)], fill=CYAN)
    im.save(tex_dir / "pad_side.png")
    previews.append(("pad_side", im))

    # 6. boost_pad_top
    im, d = new_tile(SLATE_DEEP)
    d.rectangle([0, 0, 15, 15], outline=CYAN_DIM)
    d.rectangle([1, 1, 14, 14], outline=CYAN)
    for cy in (4, 9):
        d.line([(4, cy + 3), (7, cy), (8, cy), (11, cy + 3)], fill=CYAN_BRIGHT)
        d.line([(4, cy + 4), (7, cy + 1), (8, cy + 1), (11, cy + 4)], fill=MINT)
    im.save(tex_dir / "boost_pad_top.png")
    previews.append(("boost_pad_top", im))

    # 7. timer_start_top (mint play frame)
    im, d = new_tile(SLATE_DEEP)
    d.rectangle([0, 0, 15, 15], outline=MINT_DIM)
    d.rectangle([1, 1, 14, 14], outline=MINT)
    d.polygon([(5, 4), (5, 11), (11, 8), (11, 7)], fill=MINT)
    im.save(tex_dir / "timer_start_top.png")
    previews.append(("timer_start_top", im))

    # 8. timer_end_top (coral finish checkered frame)
    im, d = new_tile(SLATE_DEEP)
    d.rectangle([0, 0, 15, 15], outline=CORAL_DIM)
    d.rectangle([1, 1, 14, 14], outline=CORAL)
    for gy in range(4, 12, 2):
        for gx in range(4, 12, 2):
            col = ICE_WHITE if ((gx + gy) // 2) % 2 == 0 else CORAL
            d.rectangle([gx, gy, gx + 1, gy + 1], fill=col)
    im.save(tex_dir / "timer_end_top.png")
    previews.append(("timer_end_top", im))

    # 9. checkpoint_pad_top (symmetric cyan diamond reticle)
    im, d = new_tile(SLATE_DEEP)
    d.rectangle([0, 0, 15, 15], outline=CYAN_DIM)
    d.rectangle([1, 1, 14, 14], outline=CYAN)
    d.polygon([(7, 3), (8, 3), (12, 7), (12, 8), (8, 12), (7, 12), (3, 8), (3, 7)], fill=CYAN)
    d.rectangle([6, 6, 9, 9], fill=ICE_WHITE)
    im.save(tex_dir / "checkpoint_pad_top.png")
    previews.append(("checkpoint_pad_top", im))

    # 10. map_wand.png (16x16 item)
    im = Image.new("RGBA", (16, 16), TRANSPARENT)
    d = ImageDraw.Draw(im)
    for i in range(3, 12):
        d.point([(i, 15 - i)], fill=STEEL_EDGE)
        d.point([(i + 1, 15 - i)], fill=SLATE_LIGHT)
    d.rectangle([2, 12, 4, 14], fill=SLATE_MID)
    # Glowing cyan zone caliper head
    d.line([(9, 4), (12, 1), (14, 3), (11, 6)], fill=CYAN)
    d.point([(12, 3), (13, 2), (11, 2)], fill=CYAN_BRIGHT)
    d.point([(10, 4)], fill=MINT)
    im.save(item_tex_dir / "map_wand.png")
    previews.append(("map_wand", im))

    # Build contact sheet for inspection
    cols = 5
    cell = 112
    rows = (len(previews) + cols - 1) // cols
    sheet = Image.new("RGBA", (cols * cell + 16, rows * cell + 16), BG_DARK)
    sd = ImageDraw.Draw(sheet)
    for idx, (label, tile) in enumerate(previews):
        r = idx // cols
        c = idx % cols
        ox = 8 + c * cell
        oy = 8 + r * cell
        sd.rectangle([ox + 2, oy + 2, ox + cell - 4, oy + cell - 4], fill=SLATE_DEEP, outline=STEEL_EDGE)
        up = tile.resize((64, 64), Image.NEAREST)
        sheet.paste(up, (ox + (cell - 64) // 2, oy + 12), up)
        sd.text((ox + 8, oy + 84), label, fill=ICE_WHITE)
    sheet.save(VERIFY / "assets_contact_sheet.png")


def write_json(path: Path, data: dict):
    path.write_text(json.dumps(data, indent=2) + "\n", encoding="utf-8")


def make_blockstates_models_items_loot():
    # Surf ramps (30, 45, 60) with 8 visual slices matching SurfRampBlock's VoxelShape
    for deg, max_h, run_px in [(30, 10, 16), (45, 16, 16), (60, 16, 10)]:
        name = f"surf_ramp_{deg}"
        top_tex = f"sourcecraft:block/surf_ramp_top_{deg}"
        side_tex = "sourcecraft:block/surf_ramp_side"

        elements = []
        slices = 8
        for i in range(slices):
            frac0 = i / slices
            frac1 = (i + 1) / slices
            p0 = round(frac0 * run_px, 2)
            p1 = round(frac1 * run_px, 2)
            h = round(max(1.0, max_h * (1.0 - frac0 * 0.88)), 2)
            # Base orientation = NORTH (-Z low, +Z high)
            z_from = round(16.0 - p1, 2)
            z_to = round(16.0 - p0, 2)
            elements.append({
                "from": [0, 0, z_from],
                "to": [16, h, z_to],
                "faces": {
                    "north": {"texture": "#side"},
                    "south": {"texture": "#side"},
                    "west": {"texture": "#side"},
                    "east": {"texture": "#side"},
                    "up": {"texture": "#top"},
                    "down": {"texture": "#side", "cullface": "down"},
                },
            })

        write_json(ASSETS / "models" / "block" / f"{name}.json", {
            "parent": "minecraft:block/block",
            "textures": {
                "particle": top_tex,
                "top": top_tex,
                "side": side_tex,
            },
            "elements": elements,
        })

        write_json(ASSETS / "blockstates" / f"{name}.json", {
            "variants": {
                "facing=north": {"model": f"sourcecraft:block/{name}"},
                "facing=east": {"model": f"sourcecraft:block/{name}", "y": 90},
                "facing=south": {"model": f"sourcecraft:block/{name}", "y": 180},
                "facing=west": {"model": f"sourcecraft:block/{name}", "y": 270},
            }
        })

    # Sleek 2px pads
    pads = [
        ("boost_pad", "sourcecraft:block/boost_pad_top", True),
        ("timer_start_pad", "sourcecraft:block/timer_start_top", False),
        ("timer_end_pad", "sourcecraft:block/timer_end_top", False),
        ("checkpoint_pad", "sourcecraft:block/checkpoint_pad_top", False),
    ]
    for name, top_tex, directional in pads:
        write_json(ASSETS / "models" / "block" / f"{name}.json", {
            "parent": "minecraft:block/block",
            "textures": {
                "particle": top_tex,
                "top": top_tex,
                "side": "sourcecraft:block/pad_side",
            },
            "elements": [
                {
                    "from": [0, 0, 0],
                    "to": [16, 2, 16],
                    "faces": {
                        "north": {"texture": "#side"},
                        "south": {"texture": "#side"},
                        "west": {"texture": "#side"},
                        "east": {"texture": "#side"},
                        "up": {"texture": "#top"},
                        "down": {"texture": "#side", "cullface": "down"},
                    },
                }
            ],
        })

        if directional:
            write_json(ASSETS / "blockstates" / f"{name}.json", {
                "variants": {
                    "facing=north": {"model": f"sourcecraft:block/{name}"},
                    "facing=east": {"model": f"sourcecraft:block/{name}", "y": 90},
                    "facing=south": {"model": f"sourcecraft:block/{name}", "y": 180},
                    "facing=west": {"model": f"sourcecraft:block/{name}", "y": 270},
                }
            })
        else:
            write_json(ASSETS / "blockstates" / f"{name}.json", {
                "variants": {
                    "": {"model": f"sourcecraft:block/{name}"}
                }
            })

    # Block item models, MC 26.3 item definitions, and loot tables
    all_blocks = [
        "surf_ramp_30", "surf_ramp_45", "surf_ramp_60",
        "boost_pad", "timer_start_pad", "timer_end_pad", "checkpoint_pad",
    ]
    for b in all_blocks:
        write_json(ASSETS / "models" / "item" / f"{b}.json", {
            "parent": f"sourcecraft:block/{b}"
        })
        write_json(ASSETS / "items" / f"{b}.json", {
            "model": {
                "type": "minecraft:model",
                "model": f"sourcecraft:item/{b}"
            }
        })
        write_json(DATA / "loot_table" / "blocks" / f"{b}.json", {
            "type": "minecraft:block",
            "pools": [
                {
                    "rolls": 1.0,
                    "bonus_rolls": 0.0,
                    "entries": [
                        {
                            "type": "minecraft:item",
                            "name": f"sourcecraft:{b}"
                        }
                    ],
                    "conditions": [
                        {
                            "condition": "minecraft:survives_explosion"
                        }
                    ]
                }
            ]
        })

    # map_wand item model & MC 26.3 item definition
    write_json(ASSETS / "models" / "item" / "map_wand.json", {
        "parent": "minecraft:item/handheld",
        "textures": {
            "layer0": "sourcecraft:item/map_wand"
        }
    })
    write_json(ASSETS / "items" / "map_wand.json", {
        "model": {
            "type": "minecraft:model",
            "model": "sourcecraft:item/map_wand"
        }
    })

    # Complete en_us.json lang file
    write_json(ASSETS / "lang" / "en_us.json", {
        "itemGroup.sourcecraft": "SourceCraft Surf & KZ",
        "block.sourcecraft.surf_ramp_30": "Surf Ramp (30° Gentle)",
        "block.sourcecraft.surf_ramp_45": "Surf Ramp (45° Classic)",
        "block.sourcecraft.surf_ramp_60": "Surf Ramp (60° Steep)",
        "block.sourcecraft.boost_pad": "Source Boost Pad",
        "block.sourcecraft.timer_start_pad": "KZ / Surf Start Pad",
        "block.sourcecraft.timer_end_pad": "KZ / Surf Finish Pad",
        "block.sourcecraft.checkpoint_pad": "KZ Checkpoint Pad",
        "item.sourcecraft.map_wand": "SourceCraft Zone Wand",
        "key.category.sourcecraft.general": "SourceCraft (CS:S Movement)",
        "key.sourcecraft.open_studio": "Open SourceCraft Studio",
        "key.sourcecraft.toggle_engine": "Toggle CS:S Physics",
        "key.sourcecraft.toggle_autobhop": "Toggle Auto-Bhop",
        "key.sourcecraft.cycle_preset": "Cycle Physics Preset",
        "key.sourcecraft.slow_walk": "Slow Walk (130 u/s)",
        "key.sourcecraft.save_cp": "Save Checkpoint (/cp)",
        "key.sourcecraft.tp_cp": "Teleport Checkpoint (/tpcp)",
        "key.sourcecraft.restart_run": "Restart Course Run (/r)",
        "key.sourcecraft.cycle_hud": "Cycle Telemetry HUD",
        "modmenu.descriptionTranslation.sourcecraft": "Authentic Counter-Strike: Source movement engine for Minecraft 26.3 — sub-stepped 100/66-tick air-strafing, bhop, surf ramps, KZ timer, telemetry HUD, and CVar console."
    })


if __name__ == "__main__":
    ensure_dirs()
    make_icon()
    make_16x16_textures()
    make_blockstates_models_items_loot()
    print("Generated SourceCraft icon, textures, models, blockstates, items, loot tables, and lang cleanly.")

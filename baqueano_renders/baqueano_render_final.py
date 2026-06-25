"""
BAQUEANO — Render producto limpio
Solo el STL ensamblado. Sin primitivas extra.
Foco: materiales aluminio anodizado + iluminación de estudio.
"""
import bpy, math, os

STL    = r"C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\box v2.stl"
OUTDIR = r"C:\Users\lukaz\OneDrive\Escritorio\baqueano_renders"
os.makedirs(OUTDIR, exist_ok=True)

# ─── ESCENA ──────────────────────────────────────────────────────────────────
bpy.ops.wm.read_factory_settings(use_empty=True)
sc = bpy.context.scene
sc.render.engine              = 'BLENDER_EEVEE'
sc.eevee.taa_render_samples   = 256
sc.render.resolution_x        = 2560
sc.render.resolution_y        = 1440
sc.render.image_settings.file_format  = 'PNG'
sc.render.image_settings.color_mode  = 'RGBA'
sc.render.film_transparent    = True
sc.unit_settings.system       = 'METRIC'
sc.unit_settings.scale_length = 0.001

world = bpy.data.worlds.new("W")
sc.world = world
world.use_nodes = True
world.node_tree.nodes["Background"].inputs["Color"].default_value    = (0,0,0,1)
world.node_tree.nodes["Background"].inputs["Strength"].default_value = 0.0

# ─── IMPORTAR STL ────────────────────────────────────────────────────────────
bpy.ops.wm.stl_import(filepath=STL, global_scale=0.001)
obj = bpy.context.selected_objects[0]
bpy.context.view_layer.objects.active = obj

# Separar las 3 partes
bpy.ops.object.mode_set(mode='EDIT')
bpy.ops.mesh.separate(type='LOOSE')
bpy.ops.object.mode_set(mode='OBJECT')

parts = list(bpy.context.selected_objects)
for p in parts:
    bpy.ops.object.select_all(action='DESELECT')
    p.select_set(True)
    bpy.context.view_layer.objects.active = p
    bpy.ops.object.origin_set(type='ORIGIN_GEOMETRY', center='BOUNDS')

# Clasificar: 8 verts=PCB placeholder, 17k=cuerpo, 64k=tapa
parts_info = sorted([(len(p.data.vertices), p) for p in parts])
pcb_obj    = parts_info[0][1]
cuerpo_obj = parts_info[1][1]
tapa_obj   = parts_info[2][1]

Hc = cuerpo_obj.dimensions.z
Ht = tapa_obj.dimensions.z

# PCB placeholder oculto
pcb_obj.hide_render = True
pcb_obj.hide_viewport = True

# ─── SHADE SMOOTH ─────────────────────────────────────────────────────────────
for ob in [cuerpo_obj, tapa_obj]:
    bpy.ops.object.select_all(action='DESELECT')
    ob.select_set(True)
    bpy.context.view_layer.objects.active = ob
    bpy.ops.object.shade_smooth()
    # Edge split para preservar bordes duros (equivalente a auto-smooth)
    es = ob.modifiers.new("EdgeSplit", 'EDGE_SPLIT')
    es.split_angle = math.radians(35)
    es.use_edge_angle = True
    es.use_edge_sharp = True

# ─── POSICIONAR ───────────────────────────────────────────────────────────────
cuerpo_obj.location = (0, 0, Hc / 2)

tapa_obj.location      = (0, 0, Hc + Ht / 2)
tapa_obj.rotation_euler = (math.radians(180), 0, 0)

H_total = Hc + Ht
Wc = cuerpo_obj.dimensions.x
Lc = cuerpo_obj.dimensions.y
mid_z = H_total / 2

print(f"Carcasa: {Wc*1000:.1f}×{Lc*1000:.1f}×{H_total*1000:.1f}mm")

# ─── MATERIALES ───────────────────────────────────────────────────────────────
def alu_mat(name, base, rough, aniso=0.0):
    m = bpy.data.materials.new(name)
    m.use_nodes = True
    nt = m.node_tree
    b  = nt.nodes["Principled BSDF"]
    b.inputs["Base Color"].default_value  = (*base, 1)
    b.inputs["Metallic"].default_value    = 1.0
    b.inputs["Roughness"].default_value   = rough
    b.inputs["Anisotropic"].default_value = aniso
    return m

# Cuerpo: aluminio anodizado negro oscuro
mat_cuerpo = alu_mat("alu_negro", (0.018, 0.018, 0.020), rough=0.22, aniso=0.4)

# Tapa: aluminio cepillado más claro
mat_tapa   = alu_mat("alu_plata", (0.55, 0.57, 0.60),   rough=0.18, aniso=0.5)

cuerpo_obj.data.materials.clear()
cuerpo_obj.data.materials.append(mat_cuerpo)
tapa_obj.data.materials.clear()
tapa_obj.data.materials.append(mat_tapa)

# ─── ILUMINACIÓN PRODUCTO ─────────────────────────────────────────────────────
def area(loc, rot_deg, energy, size, color=(1,1,1)):
    bpy.ops.object.light_add(type='AREA', location=loc)
    l = bpy.context.active_object
    l.rotation_euler = [math.radians(a) for a in rot_deg]
    l.data.energy = energy
    l.data.size   = size
    l.data.color  = color
    return l

D = max(Wc, Lc)

# Key: grande, suave, desde arriba-izquierda
area((-D*4, -D*3, D*5), (50, 0, -38),
     energy=350, size=0.50, color=(1.00, 0.97, 0.90))

# Fill: opuesto al key, muy suave
area(( D*4,  D*3, D*3), (55, 0, 140),
     energy=80,  size=0.80, color=(0.85, 0.90, 1.00))

# Rim superior: separa la tapa del fondo
area(( D*1,  D*4, D*4), (-25, 0, 155),
     energy=200, size=0.25, color=(0.75, 0.85, 1.00))

# Bottom bounce: rebote suave desde abajo
area((0, 0, -D*2), (180, 0, 0),
     energy=30,  size=1.00, color=(0.90, 0.92, 1.00))

# ─── RENDER ───────────────────────────────────────────────────────────────────
def render(fname, cam_loc, lens=60):
    bpy.ops.object.camera_add(location=cam_loc)
    c = bpy.context.active_object
    c.data.lens = lens
    # Apuntar al centro del ensamblado
    import mathutils
    target = mathutils.Vector((0, 0, mid_z))
    direction = (target - mathutils.Vector(cam_loc)).normalized()
    c.rotation_euler = direction.to_track_quat('-Z', 'Y').to_euler()
    sc.camera = c
    sc.render.filepath = os.path.join(OUTDIR, fname)
    bpy.ops.render.render(write_still=True)
    print(f"✓ {fname}")
    bpy.data.objects.remove(c)

dist = max(Wc, Lc) * 2.0

# Hero: 3/4 desde arriba, muestra tapa + borde + cuerpo
render("01_hero.png",       (-dist*0.85, -dist*1.10, mid_z + dist*0.80), lens=62)

# Lateral largo: muestra USB-C, ranuras, bisel tapa/cuerpo
render("02_lateral_usb.png",(-dist*2.0,  -dist*0.05, mid_z + dist*0.25), lens=85)

# Extremo corto: muestra los orificios del frente (SMA, PTT, conectores)
render("03_frente.png",     (-dist*0.05, -dist*2.1,  mid_z + dist*0.35), lens=85)

# 3/4 bajo: perspectiva de mano sosteniendo el dispositivo
render("04_hand_angle.png", (-dist*0.90, -dist*1.05, mid_z - dist*0.10), lens=55)

print("DONE →", OUTDIR)

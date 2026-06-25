"""
Diagnóstico: importa el STL, corrige escala (micrones→mm), render 3/4.
"""
import bpy, math

OUTPUT = r"C:\Users\lukaz\OneDrive\Escritorio\baqueano_diag.png"
STL    = r"C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\box v2.stl"

# ── Escena limpia ──────────────────────────────────────────────────────────────
bpy.ops.wm.read_factory_settings(use_empty=True)
scene = bpy.context.scene
scene.render.engine = 'BLENDER_EEVEE'
scene.render.resolution_x = 1200
scene.render.resolution_y = 900
scene.render.image_settings.file_format = 'PNG'
scene.render.filepath = OUTPUT

# Unidades en mm
scene.unit_settings.system = 'METRIC'
scene.unit_settings.scale_length = 0.001

# ── Importar STL con escala corregida ─────────────────────────────────────────
bpy.ops.wm.stl_import(filepath=STL, global_scale=0.001)
obj = bpy.context.selected_objects[0]
bpy.context.view_layer.objects.active = obj

bpy.ops.object.origin_set(type='ORIGIN_GEOMETRY', center='BOUNDS')
obj.location = (0, 0, 0)

dims = obj.dimensions
print(f"DIMS: X={dims.x*1000:.1f}mm  Y={dims.y*1000:.1f}mm  Z={dims.z*1000:.1f}mm")

# ── Material aluminio simple ───────────────────────────────────────────────────
mat = bpy.data.materials.new("aluminio")
mat.use_nodes = True
bsdf = mat.node_tree.nodes["Principled BSDF"]
bsdf.inputs["Base Color"].default_value = (0.55, 0.57, 0.60, 1)
bsdf.inputs["Metallic"].default_value = 0.95
bsdf.inputs["Roughness"].default_value = 0.25
obj.data.materials.append(mat)

# ── Cámara ─────────────────────────────────────────────────────────────────────
max_dim = max(dims.x, dims.y, dims.z)
dist = max_dim * 2.5

bpy.ops.object.camera_add(location=(dist * 0.7, -dist * 1.0, dist * 0.6))
cam = bpy.context.active_object
cam.rotation_euler = (math.radians(55), 0, math.radians(40))
scene.camera = cam

# Apuntar al objeto
cam_constraint = cam.constraints.new(type='TRACK_TO')
cam_constraint.target = obj
cam_constraint.track_axis = 'TRACK_NEGATIVE_Z'
cam_constraint.up_axis = 'UP_Y'
cam.rotation_euler = (math.radians(55), 0, math.radians(40))

# ── Luces ──────────────────────────────────────────────────────────────────────
bpy.ops.object.light_add(type='SUN', location=(dist, -dist, dist * 1.5))
sun = bpy.context.active_object
sun.data.energy = 4

bpy.ops.object.light_add(type='AREA', location=(-dist * 0.5, dist * 0.5, dist))
fill = bpy.context.active_object
fill.data.energy = 5000 * (max_dim ** 2)
fill.data.size = max_dim * 2

# Fondo gris oscuro
bpy.ops.object.light_add(type='AREA', location=(0, 0, dist * 2))
back = bpy.context.active_object
back.data.energy = 2000 * (max_dim ** 2)
back.data.size = max_dim * 3
back.data.color = (0.8, 0.85, 1.0)

# ── Render ─────────────────────────────────────────────────────────────────────
bpy.ops.render.render(write_still=True)
print(f"OK → {OUTPUT}")

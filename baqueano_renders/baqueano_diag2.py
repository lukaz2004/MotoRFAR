"""Diagnóstico: imprime info de cada parte del STL"""
import bpy, math

STL   = r"C:\Users\lukaz\OneDrive\Escritorio\MotoRFAR-MTTT\box v2.stl"
SCALE = 0.001

bpy.ops.wm.read_factory_settings(use_empty=True)
bpy.ops.wm.stl_import(filepath=STL, global_scale=SCALE)
obj = bpy.context.selected_objects[0]
bpy.context.view_layer.objects.active = obj

bpy.ops.object.mode_set(mode='EDIT')
bpy.ops.mesh.separate(type='LOOSE')
bpy.ops.object.mode_set(mode='OBJECT')

parts = list(bpy.context.selected_objects)
print(f"\n=== {len(parts)} PARTES ===")
for p in parts:
    bpy.ops.object.select_all(action='DESELECT')
    p.select_set(True)
    bpy.context.view_layer.objects.active = p
    bpy.ops.object.origin_set(type='ORIGIN_GEOMETRY', center='BOUNDS')
    d = p.dimensions
    loc = p.location
    print(f"  {p.name}: dims={d.x*1000:.1f}x{d.y*1000:.1f}x{d.z*1000:.1f}mm  "
          f"loc=({loc.x*1000:.1f},{loc.y*1000:.1f},{loc.z*1000:.1f})mm  "
          f"verts={len(p.data.vertices)}")

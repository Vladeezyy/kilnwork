#!/usr/bin/env python3
"""Rewrites NeoForge fluid, capability and registration API in Java files to Kilnwork's loader neutral API.
Usage: neo2common.py FILE... (idempotent; what it cannot rewrite is left for the compiler to point at)."""
import re, sys

IMPORTS = {
    'net.neoforged.neoforge.fluids.FluidStack': 'dev.vladeezyy.kilnwork.fluid.api.FluidStack',
    'net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction': 'dev.vladeezyy.kilnwork.fluid.api.IFluidHandler.FluidAction',
    'net.neoforged.neoforge.fluids.capability.IFluidHandlerItem': 'dev.vladeezyy.kilnwork.fluid.api.IFluidHandlerItem',
    'net.neoforged.neoforge.fluids.capability.IFluidHandler': 'dev.vladeezyy.kilnwork.fluid.api.IFluidHandler',
    'net.neoforged.neoforge.fluids.IFluidTank': 'dev.vladeezyy.kilnwork.fluid.api.IFluidTank',
    'net.neoforged.neoforge.fluids.capability.templates.FluidTank': 'dev.vladeezyy.kilnwork.fluid.api.FluidTank',
    'net.neoforged.neoforge.fluids.capability.templates.EmptyFluidHandler': 'dev.vladeezyy.kilnwork.fluid.api.EmptyFluidHandler',
    'net.neoforged.neoforge.fluids.FluidActionResult': 'dev.vladeezyy.kilnwork.fluid.api.FluidActionResult',
    'net.neoforged.neoforge.registries.DeferredBlock': 'dev.vladeezyy.kilnwork.platform.Registrar',
    'net.neoforged.neoforge.registries.DeferredItem': 'dev.vladeezyy.kilnwork.platform.Registrar',
    'net.neoforged.neoforge.registries.DeferredHolder': 'dev.vladeezyy.kilnwork.platform.Registrar',
    'net.neoforged.neoforge.registries.DeferredRegister': 'dev.vladeezyy.kilnwork.platform.Registrar',
}
PLATFORM = 'dev.vladeezyy.kilnwork.platform.KilnworkPlatform'

def rewrite(src):
    for old, new in IMPORTS.items():
        src = src.replace('import ' + old + ';', 'import ' + new + ';')
    # capabilities -> platform lookups
    src = re.sub(r'(\w+(?:\.\w+\(\))?)\.getCapability\(Capabilities\.FluidHandler\.BLOCK,\s*', r'KilnworkPlatform.get().fluidHandler(\1, ', src)
    src = re.sub(r'([\w.()]+?)\.getCapability\(Capabilities\.FluidHandler\.ITEM\)', r'KilnworkPlatform.get().fluidHandler(\1)', src)
    src = src.replace('FluidType.BUCKET_VOLUME', 'FluidStack.BUCKET_VOLUME')
    # registration types
    src = re.sub(r'\bDeferredBlock<([^<>]+(?:<[^<>]*>)?)>', r'Registrar.Entry<Block, \1>', src)
    src = re.sub(r'\bDeferredItem<([^<>]+(?:<[^<>]*>)?)>', r'Registrar.Entry<Item, \1>', src)
    src = re.sub(r'\bDeferredHolder<', 'Registrar.Entry<', src)
    if 'KilnworkPlatform.get()' in src and 'import ' + PLATFORM + ';' not in src:
        src = re.sub(r'(\npackage [^;]+;\n)', r'\1\nimport ' + PLATFORM + ';', src, count=1) if src.startswith('\n') else re.sub(r'(^package [^;]+;\n)', r'\1\nimport ' + PLATFORM + ';', src, count=1)
    # drop now unused NeoForge imports of things replaced above
    for unused in ['net.neoforged.neoforge.capabilities.Capabilities', 'net.neoforged.neoforge.fluids.FluidType']:
        name = unused.rsplit('.', 1)[1]
        if not re.search(r'\b' + name + r'\b', src.replace('import ' + unused + ';', '')):
            src = src.replace('import ' + unused + ';\n', '')
    # duplicate imports of Registrar
    lines, seen = [], set()
    for line in src.split('\n'):
        if line.startswith('import ') and line in seen:
            continue
        seen.add(line)
        lines.append(line)
    return '\n'.join(lines)

for path in sys.argv[1:]:
    with open(path, encoding='utf-8') as f:
        src = f.read()
    new = rewrite(src)
    if new != src:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(new)
        print('rewrote', path)

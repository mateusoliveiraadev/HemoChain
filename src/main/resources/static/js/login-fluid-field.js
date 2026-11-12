// Small incompressible velocity field: splat, advect, project and dissipate.
// Liquid Ether interaction reference: https://www.reactbits.dev/backgrounds/liquid-ether
// Native CPU solver keeps the existing WebGL palette and needs no dependencies.
export function createFluidField(size = 48) {
    const count = size * size;
    let x = new Float32Array(count), y = new Float32Array(count);
    let nextX = new Float32Array(count), nextY = new Float32Array(count);
    const pressure = new Float32Array(count), nextP = new Float32Array(count);
    const divergence = new Float32Array(count), pixels = new Uint8Array(count * 4);
    const at = (col, row) => Math.max(0, Math.min(size - 1, row)) * size + Math.max(0, Math.min(size - 1, col));
    function sample(field, col, row) {
        col = Math.max(0, Math.min(size - 1, col)); row = Math.max(0, Math.min(size - 1, row));
        const a = Math.floor(col), b = Math.floor(row), fx = col - a, fy = row - b;
        return (field[at(a, b)] * (1 - fx) + field[at(a + 1, b)] * fx) * (1 - fy)
            + (field[at(a, b + 1)] * (1 - fx) + field[at(a + 1, b + 1)] * fx) * fy;
    }
    function splat(ax, ay, bx, by, vx, vy, aspect) {
        const dx = (bx - ax) * aspect, dy = by - ay, length = dx * dx + dy * dy;
        for (let row = 0; row < size; row++) for (let col = 0; col < size; col++) {
            const px = col / (size - 1), py = row / (size - 1);
            const along = length ? Math.max(0, Math.min(1, ((px - ax) * aspect * dx + (py - ay) * dy) / length)) : 0;
            const rx = (px - ax - along * (bx - ax)) * aspect, ry = py - ay - along * (by - ay);
            const weight = Math.exp(-(rx * rx + ry * ry) / .006);
            const i = at(col, row);
            x[i] = Math.max(-1, Math.min(1, x[i] + vx * weight));
            y[i] = Math.max(-1, Math.min(1, y[i] + vy * weight));
        }
    }
    function step(dt) {
        const damping = Math.exp(-dt * 1.35);
        // Semi-Lagrangian backtrace transports momentum instead of following a cursor uniform.
        for (let row = 0; row < size; row++) for (let col = 0; col < size; col++) {
            const i = at(col, row), bx = col - x[i] * dt * size, by = row - y[i] * dt * size;
            nextX[i] = sample(x, bx, by) * damping;
            nextY[i] = sample(y, bx, by) * damping;
        }
        [x, nextX] = [nextX, x]; [y, nextY] = [nextY, y];
        pressure.fill(0);
        for (let row = 0; row < size; row++) for (let col = 0; col < size; col++) {
            divergence[at(col, row)] = -.5 * (x[at(col + 1, row)] - x[at(col - 1, row)] + y[at(col, row + 1)] - y[at(col, row - 1)]);
        }
        for (let pass = 0; pass < 10; pass++) {
            for (let row = 0; row < size; row++) for (let col = 0; col < size; col++) {
                nextP[at(col, row)] = (divergence[at(col, row)] + pressure[at(col - 1, row)] + pressure[at(col + 1, row)] + pressure[at(col, row - 1)] + pressure[at(col, row + 1)]) * .25;
            }
            pressure.set(nextP);
        }
        for (let row = 0; row < size; row++) for (let col = 0; col < size; col++) {
            const i = at(col, row);
            x[i] -= .5 * (pressure[at(col + 1, row)] - pressure[at(col - 1, row)]);
            y[i] -= .5 * (pressure[at(col, row + 1)] - pressure[at(col, row - 1)]);
        }
    }
    function texture() {
        for (let i = 0; i < count; i++) {
            pixels[i * 4] = Math.round((Math.max(-1, Math.min(1, x[i])) * .5 + .5) * 255);
            pixels[i * 4 + 1] = Math.round((Math.max(-1, Math.min(1, y[i])) * .5 + .5) * 255);
            pixels[i * 4 + 2] = 0; pixels[i * 4 + 3] = 255;
        }
        return pixels;
    }
    function clear() { x.fill(0); y.fill(0); }
    return {size, splat, step, texture, clear};
}

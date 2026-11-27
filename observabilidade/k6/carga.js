// Fluxo do integrante adaptado: escritas limitadas e consultas leves.
import http from 'k6/http';
import { check, fail, sleep } from 'k6';
const BASE = __ENV.BASE_URL || 'http://api:8080';
if (!/^http:\/\/(api:8080|localhost:18086|127\.0\.0\.1:18086)$/.test(BASE)) throw new Error('Somente API local de demonstracao.');
const writes = Number(__ENV.WRITE_LIMIT || 12);
const interval = Number(__ENV.READ_INTERVAL || 5);
const duration = __ENV.DURATION || '30m';
if (!Number.isInteger(writes) || writes < 1 || writes > 30 || !Number.isFinite(interval) || interval < 2 || interval > 60 || !/^([1-9]|[1-5][0-9]|60)m$/.test(duration)) throw new Error('Use 1..30 escritas, intervalo 2..60s e duracao 1m..60m.');
export const options = { vus: 1, duration, thresholds: { checks: ['rate==1'] } };
const gestor = { 'Content-Type': 'application/json', 'X-Perfil': 'GESTOR_HEMOCENTRO' };
const day = n => new Date(Date.now() + n * 86400000).toISOString().slice(0, 10);
function post(path, body, headers, expected) {
  const response = http.post(BASE + path, body === null ? null : JSON.stringify(body), { headers, tags: { name: 'POST ' + path.replace(/\d+/g, '{id}') } });
  if (!check(response, { ['status esperado ' + expected]: r => r.status === expected })) fail('Fluxo sintetico recusado: ' + path + ' status=' + response.status);
  return response;
}
export function setup() {
  return { hospitalId: post('/hospitais', { nome: 'Hospital sintetico RSD ' + Date.now() }, gestor, 200).json('id') };
}
export default function (data) {
  const hospital = { ...gestor, 'X-Perfil': 'HOSPITAL_SOLICITANTE', 'X-Hospital-Id': String(data.hospitalId) };
  if (__ITER < writes) {
    const bolsa = post('/bolsas', { codigo: 'RSD-' + Date.now() + '-' + __ITER, tipoComponente: 'CONCENTRADO_HEMACIAS', tipoSanguineo: 'O_NEGATIVO', volumeMl: 450, dataColeta: day(-1) }, gestor, 201).json('id');
    const req = post('/requisicoes', { hospitalId: data.hospitalId, tipoComponente: 'CONCENTRADO_HEMACIAS', tipoSanguineo: 'A_POSITIVO', quantidade: 1, urgencia: 'ROTINA', prazoLimite: day(7) }, hospital, 200).json('id');
    const entrega = post('/requisicoes/' + req + '/entregas', { bolsaId: bolsa, entregaRealizada: true }, gestor, 201).json('id');
    const base = '/requisicoes/' + req + '/entregas/' + entrega;
    post(base + '/recebimento', { condicao: 'INTEGRA' }, hospital, 200);
    post(base + '/utilizacao', null, hospital, 200);
    // Recusa deterministica: quantidade solicitada ja atendida.
    post('/requisicoes/' + req + '/entregas', { bolsaId: bolsa, entregaRealizada: true }, gestor, 409);
  }
  const response = http.get(BASE + '/hospitais', { headers: gestor });
  check(response, { 'leitura valida': r => r.status === 200 });
  if (__ITER % 12 === 0) {
    const error = http.get(BASE + '/observabilidade/erro-simulado');
    check(error, { 'erro controlado 500': r => r.status === 500 });
  }
  sleep(interval);
}

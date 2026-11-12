import {startLoginLiquid} from './login-liquid.js';
if(document.body.classList.contains('hemo-login')){
    const title=document.getElementById('titulo'),intro=document.getElementById('loginIntro'),toggle=document.getElementById('btnAlternar'),action=document.getElementById('btnAcao'),name=document.getElementById('campoNome'),password=document.getElementById('inputSenha'),status=document.getElementById('statusMsg');
    function mode(){const signup=!name.classList.contains('hidden');title.textContent=signup?'Crie sua conta':'Acesse sua conta';intro.textContent=signup?'Cadastre seus dados para começar.':'Seu acesso à gestão de hemocomponentes.';toggle.textContent=signup?'Já tenho conta. Entrar':'Não tenho conta. Cadastrar';toggle.setAttribute('aria-expanded',String(signup));password.autocomplete=signup?'new-password':'current-password';}
    function feedback(){const text=status.textContent.trim(),busy=text==='Processando...';action.disabled=busy;action.setAttribute('aria-busy',String(busy));status.dataset.tone=busy?'progress':/conta criada/i.test(text)?'success':text?'error':'empty';}
    new MutationObserver(mode).observe(name,{attributes:true,attributeFilter:['class']});
    new MutationObserver(feedback).observe(status,{childList:true,characterData:true,subtree:true});mode();feedback();
    const form=document.getElementById('loginForm');form.addEventListener('submit',event=>{event.preventDefault();if(!action.disabled)action.click();});
    form.addEventListener('keydown',event=>{if(event.key==='Enter'&&!event.isComposing&&event.target.matches('input')){event.preventDefault();if(!action.disabled)action.click();}});
    const email=document.getElementById('inputEmail'),hint=document.getElementById('emailHint');
    function validity(){const invalid=!!email.value&&!email.validity.valid;email.setAttribute('aria-invalid',String(invalid));hint.hidden=!invalid;hint.textContent=invalid?email.validationMessage:'';}
    email.addEventListener('blur',validity);email.addEventListener('input',()=>{if(email.getAttribute('aria-invalid')==='true')validity();});
    startLoginLiquid(document.querySelector('.login-liquid canvas'));
}

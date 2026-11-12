// P4 presents existing native validity; it never prevents events or changes constraints.
const main=document.querySelector('body.hemo-internal #conteudo');
if(main){
    const messages=new WeakMap();let nextId=0;
    function clear(field){
        const error=messages.get(field);if(!error)return;
        const ids=(field.getAttribute('aria-describedby')||'').split(/\s+/).filter(id=>id&&id!==error.id);
        if(ids.length)field.setAttribute('aria-describedby',ids.join(' '));else field.removeAttribute('aria-describedby');
        field.removeAttribute('aria-invalid');error.remove();messages.delete(field);
    }
    main.addEventListener('invalid',event=>{
        const field=event.target;if(!field.matches('input,select,textarea'))return;
        let error=messages.get(field);
        if(!error){error=document.createElement('p');error.id=`hemo-field-error-${++nextId}`;error.className='hemo-field-error';field.insertAdjacentElement('afterend',error);messages.set(field,error);field.setAttribute('aria-describedby',`${field.getAttribute('aria-describedby')||''} ${error.id}`.trim());}
        field.setAttribute('aria-invalid','true');error.textContent=field.validationMessage;
    },true);
    const update=event=>{if(event.target.matches('input,select,textarea')&&event.target.validity.valid)clear(event.target);};
    main.addEventListener('input',update);main.addEventListener('change',update);
    main.addEventListener('reset',event=>queueMicrotask(()=>event.target.querySelectorAll('input,select,textarea').forEach(clear)));
    function decorate(){main.querySelectorAll('input[required],select[required],textarea[required]').forEach(field=>{[...field.labels||[]].forEach(label=>{if(label.querySelector('.hemo-required'))return;const mark=document.createElement('span');mark.className='hemo-required';mark.setAttribute('aria-hidden','true');mark.textContent=' *';label.appendChild(mark);});});}
    let pending=false;new MutationObserver(()=>{if(!pending){pending=true;queueMicrotask(()=>{pending=false;decorate();});}}).observe(main,{childList:true,subtree:true});decorate();
}

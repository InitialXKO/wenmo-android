//! Offline, deterministic input engine shared by every Wenmo platform shell.

use std::collections::HashMap;
use std::sync::OnceLock;

static DICTIONARY: OnceLock<Dictionary> = OnceLock::new();

pub struct Dictionary {
    simplified: HashMap<String, Vec<String>>,
    traditional: HashMap<String, Vec<String>>,
}

impl Dictionary {
    pub fn get_global() -> &'static Dictionary {
        DICTIONARY.get_or_init(|| {
            let content = include_str!("../../app/src/main/assets/cedict_pinyin.tsv");
            let mut simplified = HashMap::new();
            let mut traditional = HashMap::new();

            for line in content.lines() {
                if line.is_empty() || line.starts_with('#') {
                    continue;
                }
                let mut parts = line.split('\t');
                if let (Some(pinyin), Some(simp), Some(trad)) = (parts.next(), parts.next(), parts.next()) {
                    let key = pinyin.to_string();
                    let simp_list: &mut Vec<String> = simplified.entry(key.clone()).or_default();
                    if simp_list.len() < 32 && !simp_list.iter().any(|s| s == simp) {
                        simp_list.push(simp.to_string());
                    }
                    let trad_list: &mut Vec<String> = traditional.entry(key).or_default();
                    if trad_list.len() < 32 && !trad_list.iter().any(|s| s == trad) {
                        trad_list.push(trad.to_string());
                    }
                }
            }

            Dictionary { simplified, traditional }
        })
    }

    pub fn lookup(&self, pinyin: &str, is_traditional: bool) -> Vec<String> {
        let map = if is_traditional { &self.traditional } else { &self.simplified };
        map.get(pinyin).cloned().unwrap_or_default()
    }
}

#[derive(Clone, Copy, Debug, Default, Eq, PartialEq)]
pub enum Script {
    #[default]
    Simplified,
    Traditional,
}

#[derive(Debug, Default)]
pub struct Engine {
    composition: String,
    script: Script,
}

impl Engine {
    pub fn new() -> Self { Self::default() }

    pub fn type_ascii(&mut self, ch: char) {
        if ch.is_ascii_lowercase() { self.composition.push(ch); }
    }

    pub fn backspace(&mut self) { self.composition.pop(); }
    pub fn clear(&mut self) { self.composition.clear(); }
    pub fn composition(&self) -> &str { &self.composition }
    pub fn set_script(&mut self, script: Script) { self.script = script; }
    pub fn is_traditional(&self) -> bool { matches!(self.script, Script::Traditional) }

    pub fn candidates(&self) -> Vec<String> {
        if self.composition.is_empty() {
            return Vec::new();
        }
        Dictionary::get_global().lookup(&self.composition, self.is_traditional())
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn composition_and_script_are_deterministic() {
        let mut engine = Engine::new();
        for ch in "ni".chars() { engine.type_ascii(ch); }
        assert_eq!(engine.composition(), "ni");
        assert!(engine.candidates().contains(&"你".to_string()));
        engine.set_script(Script::Traditional);
        assert!(engine.candidates().contains(&"你".to_string()));
    }

    #[test]
    fn ignores_non_lowercase_input() {
        let mut engine = Engine::new();
        for ch in "Ni你1".chars() { engine.type_ascii(ch); }
        assert_eq!(engine.composition(), "i");
    }
}

pub mod jni_api {
    use super::{Engine, Script};
    use jni::objects::{JClass, JObjectArray, JString};
    use jni::sys::{jboolean, jchar, jlong};
    use jni::JNIEnv;

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_ink_wenmo_ime_engine_RustInputEngine_nativeCreate(
        _env: JNIEnv,
        _class: JClass,
    ) -> jlong {
        let engine = Box::new(Engine::new());
        Box::into_raw(engine) as jlong
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_ink_wenmo_ime_engine_RustInputEngine_nativeDestroy(
        _env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) {
        if handle != 0 {
            unsafe {
                let _ = Box::from_raw(handle as *mut Engine);
            }
        }
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_ink_wenmo_ime_engine_RustInputEngine_nativeType(
        _env: JNIEnv,
        _class: JClass,
        handle: jlong,
        ch: jchar,
    ) {
        if let Some(ch) = char::from_u32(ch as u32) {
            if handle != 0 {
                let engine = unsafe { &mut *(handle as *mut Engine) };
                engine.type_ascii(ch);
            }
        }
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_ink_wenmo_ime_engine_RustInputEngine_nativeBackspace(
        _env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) {
        if handle != 0 {
            let engine = unsafe { &mut *(handle as *mut Engine) };
            engine.backspace();
        }
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_ink_wenmo_ime_engine_RustInputEngine_nativeClear(
        _env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) {
        if handle != 0 {
            let engine = unsafe { &mut *(handle as *mut Engine) };
            engine.clear();
        }
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_ink_wenmo_ime_engine_RustInputEngine_nativeComposition<'local>(
        env: JNIEnv<'local>,
        _class: JClass,
        handle: jlong,
    ) -> JString<'local> {
        let comp = if handle != 0 {
            let engine = unsafe { &*(handle as *const Engine) };
            engine.composition()
        } else {
            ""
        };
        env.new_string(comp).unwrap_or_else(|_| env.new_string("").unwrap())
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_ink_wenmo_ime_engine_RustInputEngine_nativeCandidates<'local>(
        mut env: JNIEnv<'local>,
        _class: JClass,
        handle: jlong,
    ) -> JObjectArray<'local> {
        let candidates = if handle != 0 {
            let engine = unsafe { &*(handle as *const Engine) };
            engine.candidates()
        } else {
            Vec::new()
        };

        let string_class = match env.find_class("java/lang/String") {
            Ok(cls) => cls,
            Err(_) => return JObjectArray::from(jni::objects::JObject::null()),
        };

        let array = match env.new_object_array(candidates.len() as i32, &string_class, JString::default()) {
            Ok(arr) => arr,
            Err(_) => return JObjectArray::from(jni::objects::JObject::null()),
        };

        for (i, candidate) in candidates.iter().enumerate() {
            if let Ok(jstr) = env.new_string(candidate) {
                let _ = env.set_object_array_element(&array, i as i32, jstr);
            }
        }

        array
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_ink_wenmo_ime_engine_RustInputEngine_nativeSetTraditional(
        _env: JNIEnv,
        _class: JClass,
        handle: jlong,
        traditional: jboolean,
    ) {
        if handle != 0 {
            let engine = unsafe { &mut *(handle as *mut Engine) };
            engine.set_script(if traditional != 0 {
                Script::Traditional
            } else {
                Script::Simplified
            });
        }
    }

    #[unsafe(no_mangle)]
    pub extern "system" fn Java_ink_wenmo_ime_engine_RustInputEngine_nativeIsTraditional(
        _env: JNIEnv,
        _class: JClass,
        handle: jlong,
    ) -> jboolean {
        if handle != 0 {
            let engine = unsafe { &*(handle as *const Engine) };
            if engine.is_traditional() {
                1
            } else {
                0
            }
        } else {
            0
        }
    }
}

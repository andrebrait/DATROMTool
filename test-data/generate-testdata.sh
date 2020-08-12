#!/bin/bash

rm -rf data 2> /dev/null
mkdir -p data
cd data || exit 1

for i in $(seq 9 22); do
    c="$((2 ** i))"
    file_name="$(printf "%07d" "${c}")"
    mkdir -p "${file_name}"
    echo "Generating ${file_name}.txt and headered-${file_name}.txt"
    base64 /dev/urandom | head -c "${c}" > "${file_name}/${file_name}.txt"
    mkdir -p "${file_name}/headered"
    printf "\x4E\x45\x53\x1A%s" "$(base64 /dev/zero | head -c 12)" \
        | cat - "${file_name}/${file_name}.txt" > "${file_name}/headered/headered-${file_name}.txt"
done

c="$((c + 1))"
file_name="$(printf "%07d" "${c}")"
mkdir -p "${file_name}"
echo "Generating ${file_name}.txt and headered-${file_name}.txt"
base64 /dev/urandom | head -c "${c}" > "${file_name}/${file_name}.txt"
mkdir -p "${file_name}/headered"
printf "\x4E\x45\x53\x1A%s" "$(base64 /dev/zero | head -c 12)" \
    | cat - "${file_name}/${file_name}.txt" > "${file_name}/headered/headered-${file_name}.txt"

shopt -s globstar

for i in **/*.txt; do sha1sum "$i" >> SHA1SUMS; done

for i in **/*.txt; do md5sum "$i" >> MD5SUMS; done

for i in **/*.txt; do python3 -c "import sys; a=sys.argv[1].split(' '); h=hex(int(a[0])); print(h.lstrip('0').lstrip('x'), a[1], a[2])" "$(cksum "${i}")" >> CRC32SUMS; done

for i in **/*.txt; do
    echo "Compressing ${i}"
    echo "Compressing ${i}.zip" && zip -q "${i}.zip" "${i}"
    echo "Compressing ${i}.rar" && rar a "${i}.rar" "${i}" > /dev/null
    echo "Compressing ${i}.rar4.rar" && cp ../rar4.rar "./${i}.rar4.rar" \
        && rar a "${i}.rar4.rar" "${i}" > /dev/null \
        && rar d "${i}.rar4.rar" "dummy.txt" > /dev/null
    echo "Compressing ${i}.7z" && 7z a "${i}.7z" "${i}" > /dev/null
    echo "Compressing ${i}.tar" && tar cf "${i}.tar" "${i}"
    echo "Compressing ${i}.tar.bz2/tar.bz/tbz2/tbz" && tar cf "${i}.tar.bz2" "${i}" --bzip2 \
        && cp "${i}.tar.bz2" "${i}.tar.bz" \
        && cp "${i}.tar.bz2" "${i}.tbz2" \
        && cp "${i}.tar.bz2" "${i}.tbz"
    echo "Compressing ${i}.tar.gz/tar.z/tgz/taz" && tar cf "${i}.tar.gz" "${i}" --gzip \
        && cp "${i}.tar.gz" "${i}.tar.z" \
        && cp "${i}.tar.gz" "${i}.tgz" \
        && cp "${i}.tar.gz" "${i}.taz"
    echo "Compressing ${i}.tar.lz4" && tar cf "${i}.tar.lz4" "${i}" -I lz4
    echo "Compressing ${i}.tar.lzma" && tar cf "${i}.tar.lzma" "${i}" -I lzma
    echo "Compressing ${i}.tar.xz/txz" && tar cf "${i}.tar.xz" "${i}" --xz \
        && cp "${i}.tar.xz" "${i}.txz"
done
